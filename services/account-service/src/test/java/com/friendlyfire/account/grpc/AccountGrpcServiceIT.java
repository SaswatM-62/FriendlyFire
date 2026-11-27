package com.friendlyfire.account.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.friendlyfire.account.rso.RiotAccount;
import com.friendlyfire.account.testsupport.FakeMatchDataClient;
import com.friendlyfire.account.testsupport.FakeRsoClient;
import com.friendlyfire.account.testsupport.FakeScoringClient;
import com.friendlyfire.account.testsupport.FakesConfig;
import com.friendlyfire.account.testsupport.MutableClock;
import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc;
import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.DataStatus;
import com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.FinishOnboardingRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.GetDataStatusRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.RiotLink;
import com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.Look;
import com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.UpdateLookRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.User;
import com.friendlyfire.proto.friendlyfire.account.v1.UserDataStatus;
import com.friendlyfire.proto.friendlyfire.matchdata.v1.PlayerSyncStatus;
import com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncStatus;
import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Every AccountService RPC over a real gRPC server, against a real Postgres, with Riot
 * Sign-On, Match Data, Scoring and the clock replaced by fakes (FakesConfig).
 */
@SpringBootTest(properties = {
    "spring.grpc.server.port=0",
    "spring.grpc.client.channel.matchdata.target=localhost:1",
    "spring.grpc.client.channel.scoring.target=localhost:1",
})
@Import(FakesConfig.class)
@Testcontainers
class AccountGrpcServiceIT {

    private static final String CALLBACK = "http://localhost:3000/link/riot/callback";

    // Same image as compose.yaml.
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16.15-alpine");

    @LocalGrpcServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private FakeRsoClient rso;

    @Autowired
    private FakeMatchDataClient matchData;

    @Autowired
    private FakeScoringClient scoring;

    @Autowired
    private MutableClock clock;

    private ManagedChannel channel;
    private AccountServiceBlockingStub accounts;

    @BeforeEach
    void connect() {
        // The tests share one database and reuse Riot accounts ("puuid-1"), so each starts with no links.
        jdbc.update("DELETE FROM account.riot_links");
        rso.reset();
        matchData.reset();
        scoring.reset();
        clock.set(FakesConfig.NOW);
        channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
        accounts = AccountServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void disconnect() {
        channel.shutdownNow();
    }

    // ---------- EnsureUser and GetUser ----------

    @Test
    void ensureUserCreatesTheUserOnceAndKeepsTheirFirstDetails() {
        EnsureUserRequest request = EnsureUserRequest.newBuilder()
                .setAuthSubject("auth0|" + UUID.randomUUID())
                .setEmail("player@example.test")
                .setSuggestedDisplayName("Sam")
                .build();

        EnsureUserResponse first = accounts.ensureUser(request);
        EnsureUserResponse second = accounts.ensureUser(request.toBuilder()
                .setEmail("changed@example.test")
                .setSuggestedDisplayName("Changed Name")
                .build());

        assertThat(first.getCreated()).isTrue();
        assertThat(first.getUser().getEmail()).isEqualTo("player@example.test");
        assertThat(first.getUser().getDisplayName()).isEqualTo("Sam");
        assertThat(first.getUser().hasCreatedAt()).isTrue();
        assertThat(second.getCreated()).isFalse();
        assertThat(second.getUser()).isEqualTo(first.getUser());
    }

    @Test
    void ensureUserNeverUsesAnEmailAddressAsTheName() {
        EnsureUserResponse response = accounts.ensureUser(EnsureUserRequest.newBuilder()
                .setAuthSubject("auth0|" + UUID.randomUUID())
                .setEmail("sam@example.test")
                .setSuggestedDisplayName("sam@example.test")
                .build());

        assertThat(response.getUser().getDisplayName()).matches("Player \\d{4}");
    }

    @Test
    void getUserWithoutARiotLinkHasNone() {
        User user = newUser("Sam");

        User got = accounts.getUser(GetUserRequest.newBuilder().setUserId(user.getUserId()).build()).getUser();

        assertThat(got.getUserId()).isEqualTo(user.getUserId());
        assertThat(got.hasRiotLink()).isFalse();
    }

    @Test
    void getUserForAnUnknownUserIsNotFound() {
        assertStatus(() -> accounts.getUser(GetUserRequest.newBuilder()
                .setUserId(UUID.randomUUID().toString()).build()), Status.Code.NOT_FOUND);
    }

    // ---------- Linking a Riot account ----------

    @Test
    void linkingSavesTheLinkWithoutThePuuidAndStartsASync() {
        User user = newUser("Sam");
        String state = startLink(user);
        rso.codes.put("code-1", new RiotAccount("puuid-sam", "Sam", "NA1", "na"));

        RiotLink link = accounts.completeRiotLink(complete(user, "code-1", state)).getRiotLink();

        assertThat(link.getGameName()).isEqualTo("Sam");
        assertThat(link.getTagLine()).isEqualTo("NA1");
        assertThat(link.getRegion()).isEqualTo("na");
        assertThat(link.getLinkedAt().getSeconds()).isEqualTo(FakesConfig.NOW.getEpochSecond());
        assertThat(matchData.syncsStarted).containsExactly(uuid(user));
        // EnsureUser is what the Gateway's GET /api/me shows, so it carries the link too.
        User ensured = accounts.ensureUser(EnsureUserRequest.newBuilder().setAuthSubject(subjectOf(user)).build())
                .getUser();
        User got = accounts.getUser(GetUserRequest.newBuilder().setUserId(user.getUserId()).build()).getUser();
        for (User returned : List.of(ensured, got)) {
            assertThat(returned.getRiotLink()).isEqualTo(link);
            assertThat(returned.toString()).doesNotContain("puuid-sam");
        }
    }

    @Test
    void theAuthorizeUrlComesFromRiotSignOnWithANewState() {
        User user = newUser("Sam");

        String url = accounts.startRiotLink(StartRiotLinkRequest.newBuilder()
                .setUserId(user.getUserId()).setRedirectUri(CALLBACK).build()).getAuthorizeUrl();

        assertThat(url).startsWith(FakeRsoClient.AUTHORIZE).contains("redirect_uri=" + CALLBACK);
        assertThat(FakeRsoClient.stateIn(url)).isNotBlank();
        // The account's email goes along, for the mock's check of the Riot ID.
        assertThat(url).endsWith("login_hint=" + user.getEmail());
    }

    @Test
    void startingALinkForAnUnknownUserIsNotFound() {
        assertStatus(() -> accounts.startRiotLink(StartRiotLinkRequest.newBuilder()
                .setUserId(UUID.randomUUID().toString()).setRedirectUri(CALLBACK).build()), Status.Code.NOT_FOUND);
    }

    @Test
    void aLinkRequestWorksOnceAndOnlyForWhoStartedIt() {
        User maya = newUser("Maya");
        User sam = newUser("Sam");
        rso.codes.put("code-1", new RiotAccount("puuid-sam", "Sam", "NA1", "na"));

        String mayasState = startLink(maya);
        assertStatus(() -> accounts.completeRiotLink(complete(sam, "code-1", mayasState)), Status.Code.INVALID_ARGUMENT);
        // Trying it used it up, even for Maya.
        assertStatus(() -> accounts.completeRiotLink(complete(maya, "code-1", mayasState)),
                Status.Code.INVALID_ARGUMENT);

        String samsState = startLink(sam);
        accounts.completeRiotLink(complete(sam, "code-1", samsState));
        assertStatus(() -> accounts.completeRiotLink(complete(sam, "code-1", samsState)), Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void aLinkRequestExpiresAfterTenMinutes() {
        User user = newUser("Sam");
        String state = startLink(user);
        rso.codes.put("code-1", new RiotAccount("puuid-sam", "Sam", "NA1", "na"));

        clock.advance(Duration.ofMinutes(10));

        assertStatus(() -> accounts.completeRiotLink(complete(user, "code-1", state)), Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void aCodeRiotRejectsIsInvalidAndRiotBeingDownIsUnavailable() {
        User user = newUser("Sam");

        assertStatus(() -> accounts.completeRiotLink(complete(user, "unknown-code", startLink(user))),
                Status.Code.INVALID_ARGUMENT);
        rso.down = true;
        assertStatus(() -> accounts.completeRiotLink(complete(user, "code-1", startLink(user))),
                Status.Code.UNAVAILABLE);
        assertThat(linkCount(user)).isZero();
    }

    @Test
    void aRiotAccountLinkedToSomeoneElseIsAlreadyExists() {
        User maya = linkedUser("Maya", "puuid-shared");
        User sam = newUser("Sam");
        rso.codes.put("code-2", new RiotAccount("puuid-shared", "Maya", "EUW", "eu"));

        assertStatus(() -> accounts.completeRiotLink(complete(sam, "code-2", startLink(sam))),
                Status.Code.ALREADY_EXISTS);
        assertThat(linkCount(sam)).isZero();
        assertThat(linkCount(maya)).isOne();
    }

    @Test
    void linkingStillWorksWhenTheSyncCantStart() {
        User user = newUser("Sam");
        String state = startLink(user);
        rso.codes.put("code-1", new RiotAccount("puuid-sam", "Sam", "NA1", "na"));
        matchData.down = true;

        accounts.completeRiotLink(complete(user, "code-1", state));

        assertThat(linkCount(user)).isOne();
    }

    @Test
    void linkingADifferentRiotAccountDeletesTheOldMatchesFirst() {
        User user = linkedUser("Sam", "puuid-old");
        rso.codes.put("code-new", new RiotAccount("puuid-new", "Sammy", "NA2", "na"));

        matchData.down = true;
        assertStatus(() -> accounts.completeRiotLink(complete(user, "code-new", startLink(user))),
                Status.Code.UNAVAILABLE);
        assertThat(puuidOf(user)).isEqualTo("puuid-old");

        matchData.down = false;
        accounts.completeRiotLink(complete(user, "code-new", startLink(user)));
        assertThat(matchData.deleted).containsExactly(uuid(user));
        assertThat(puuidOf(user)).isEqualTo("puuid-new");
    }

    @Test
    void relinkingTheSameRiotAccountKeepsItsMatches() {
        User user = linkedUser("Sam", "puuid-sam");
        rso.codes.put("code-again", new RiotAccount("puuid-sam", "Sam Renamed", "NA1", "na"));

        RiotLink link = accounts.completeRiotLink(complete(user, "code-again", startLink(user))).getRiotLink();

        assertThat(link.getGameName()).isEqualTo("Sam Renamed");
        assertThat(matchData.deleted).isEmpty();
    }

    // ---------- Display name ----------

    @Test
    void aNewNameIsTrimmedAndBadNamesAreRefused() {
        User user = newUser("Sam");

        User renamed = accounts.updateDisplayName(rename(user.getUserId(), "  Sam Rivera  ")).getUser();
        assertThat(renamed.getDisplayName()).isEqualTo("Sam Rivera");

        assertStatus(() -> accounts.updateDisplayName(rename(user.getUserId(), "   ")), Status.Code.INVALID_ARGUMENT);
        assertStatus(() -> accounts.updateDisplayName(rename(user.getUserId(), "a".repeat(25))),
                Status.Code.INVALID_ARGUMENT);
        assertStatus(() -> accounts.updateDisplayName(rename(UUID.randomUUID().toString(), "Nobody")),
                Status.Code.NOT_FOUND);
    }

    // ---------- UpdateLook (FF-519) ----------

    @Test
    void aSavedLookComesBackWithTheUser() {
        User user = newUser("Sam");
        assertThat(user.hasLook()).as("no look until they pick one").isFalse();

        User updated = accounts.updateLook(lookOf(user.getUserId(), "ghost", "cyan", "locked-in")).getUser();

        assertThat(updated.getLook()).isEqualTo(Look.newBuilder()
                .setIcon("ghost").setColour("cyan").setExpression("locked-in").build());
        assertThat(accounts.getUser(GetUserRequest.newBuilder().setUserId(user.getUserId()).build())
                .getUser().getLook().getIcon()).isEqualTo("ghost");
    }

    @Test
    void aLookThatIsNotOnOfferIsRefused() {
        User user = newUser("Sam");

        assertStatus(() -> accounts.updateLook(lookOf(user.getUserId(), "dragon", "cyan", "happy")),
                Status.Code.INVALID_ARGUMENT);
        assertStatus(() -> accounts.updateLook(lookOf(user.getUserId(), "ghost", "teal", "happy")),
                Status.Code.INVALID_ARGUMENT);
        assertStatus(() -> accounts.updateLook(lookOf(user.getUserId(), "ghost", "cyan", "wow")),
                Status.Code.INVALID_ARGUMENT);
        assertStatus(() -> accounts.updateLook(UpdateLookRequest.newBuilder().setUserId(user.getUserId()).build()),
                Status.Code.INVALID_ARGUMENT);
        assertStatus(() -> accounts.updateLook(lookOf(UUID.randomUUID().toString(), "ghost", "cyan", "happy")),
                Status.Code.NOT_FOUND);
        assertStatus(() -> accounts.updateLook(lookOf("not-a-uuid", "ghost", "cyan", "happy")),
                Status.Code.NOT_FOUND);
    }

    @Test
    void aNewNameKeepsTheRiotLink() {
        User user = linkedUser("Sam", "puuid-sam");

        User renamed = accounts.updateDisplayName(rename(user.getUserId(), "Sammy")).getUser();

        assertThat(renamed.hasRiotLink()).isTrue();
    }

    // ---------- Data status ----------

    @Test
    void theDataStatusFollowsTheTwentyFourHourRule() {
        User unlinked = newUser("Maya");
        User linked = linkedUser("Sam", "puuid-sam");
        matchData.statuses.put(uuid(linked), syncStatus(SyncStatus.SYNC_STATUS_COMPLETE, Duration.ofHours(1)));

        List<UserDataStatus> statuses = statusesOf(linked.getUserId(), unlinked.getUserId(),
                UUID.randomUUID().toString(), "not-a-uuid");

        assertThat(statuses).extracting(UserDataStatus::getStatus).containsExactly(
                DataStatus.DATA_STATUS_SYNCED,
                DataStatus.DATA_STATUS_NOT_LINKED,
                DataStatus.DATA_STATUS_NOT_LINKED,
                DataStatus.DATA_STATUS_NOT_LINKED);
        assertThat(statuses.getFirst().getMatchesStored()).isEqualTo(41);

        clock.advance(Duration.ofHours(23));
        assertThat(statusesOf(linked.getUserId()).getFirst().getStatus()).isEqualTo(DataStatus.DATA_STATUS_NOT_SYNCED);
    }

    @Test
    void aRunningSyncShowsItsEstimate() {
        User linked = linkedUser("Sam", "puuid-sam");
        matchData.statuses.put(uuid(linked), PlayerSyncStatus.newBuilder()
                .setUserId(linked.getUserId()).setStatus(SyncStatus.SYNC_STATUS_IN_PROGRESS).setEstimatedSeconds(90).build());

        UserDataStatus status = statusesOf(linked.getUserId()).getFirst();

        assertThat(status.getStatus()).isEqualTo(DataStatus.DATA_STATUS_SYNCING);
        assertThat(status.getEstimatedSeconds()).isEqualTo(90);
    }

    @Test
    void theDataStatusIsUnavailableOnlyWhenALinkedPlayerNeedsMatchData() {
        User unlinked = newUser("Maya");
        User linked = linkedUser("Sam", "puuid-sam");
        matchData.down = true;

        assertThat(statusesOf(unlinked.getUserId()).getFirst().getStatus())
                .isEqualTo(DataStatus.DATA_STATUS_NOT_LINKED);
        assertStatus(() -> statusesOf(linked.getUserId()), Status.Code.UNAVAILABLE);
    }

    // ---------- The sign-in sync ----------

    @Test
    void signInStartsASyncOnlyWhenTheDataIsStale() {
        User user = linkedUser("Sam", "puuid-sam");
        matchData.syncsStarted.clear();
        EnsureUserRequest signIn = EnsureUserRequest.newBuilder().setAuthSubject(subjectOf(user)).build();

        matchData.statuses.put(uuid(user), syncStatus(SyncStatus.SYNC_STATUS_COMPLETE, Duration.ofHours(1)));
        accounts.ensureUser(signIn);
        assertThat(matchData.syncsStarted).isEmpty();

        matchData.statuses.put(uuid(user), syncStatus(SyncStatus.SYNC_STATUS_COMPLETE, Duration.ofHours(25)));
        accounts.ensureUser(signIn);
        assertThat(matchData.syncsStarted).containsExactly(uuid(user));
    }

    @Test
    void signInWorksWhenMatchDataIsDown() {
        User user = linkedUser("Sam", "puuid-sam");
        matchData.down = true;

        User signedIn = accounts.ensureUser(EnsureUserRequest.newBuilder().setAuthSubject(subjectOf(user)).build())
                .getUser();

        assertThat(signedIn.getUserId()).isEqualTo(user.getUserId());
    }

    // ---------- Unlinking ----------

    @Test
    void unlinkingDeletesTheMatchesThenTheLink() {
        User user = linkedUser("Sam", "puuid-sam");

        accounts.unlinkRiot(UnlinkRiotRequest.newBuilder().setUserId(user.getUserId()).build());

        assertThat(matchData.deleted).containsExactly(uuid(user));
        assertThat(linkCount(user)).isZero();
        // Nothing left to unlink: still fine.
        accounts.unlinkRiot(UnlinkRiotRequest.newBuilder().setUserId(user.getUserId()).build());
    }

    @Test
    void unlinkingKeepsTheLinkWhenMatchDataCantDelete() {
        User user = linkedUser("Sam", "puuid-sam");
        matchData.down = true;

        assertStatus(() -> accounts.unlinkRiot(UnlinkRiotRequest.newBuilder().setUserId(user.getUserId()).build()),
                Status.Code.UNAVAILABLE);
        assertThat(linkCount(user)).isOne();
    }

    // ---------- Deleting the account ----------

    @Test
    void deletingTheAccountClearsMatchesAnonymizesHistoryAndRemovesTheUser() {
        User user = linkedUser("Sam", "puuid-sam");

        accounts.deleteAccount(DeleteAccountRequest.newBuilder().setUserId(user.getUserId()).build());

        assertThat(matchData.deleted).containsExactly(uuid(user));
        assertThat(scoring.anonymized).containsExactly(uuid(user));
        assertThat(linkCount(user)).isZero();
        assertStatus(() -> accounts.getUser(GetUserRequest.newBuilder().setUserId(user.getUserId()).build()),
                Status.Code.NOT_FOUND);
        // Already gone: still fine.
        accounts.deleteAccount(DeleteAccountRequest.newBuilder().setUserId(user.getUserId()).build());
    }

    @Test
    void aPlayerWhoNeverLinkedIsDeletedWithoutAskingMatchData() {
        User user = newUser("Maya");
        matchData.down = true;

        accounts.deleteAccount(DeleteAccountRequest.newBuilder().setUserId(user.getUserId()).build());

        assertThat(scoring.anonymized).containsExactly(uuid(user));
    }

    @Test
    void deletingStopsAndKeepsTheUserWhenAServiceCantBeReached() {
        User user = linkedUser("Sam", "puuid-sam");
        DeleteAccountRequest delete = DeleteAccountRequest.newBuilder().setUserId(user.getUserId()).build();

        scoring.down = true;
        assertStatus(() -> accounts.deleteAccount(delete), Status.Code.UNAVAILABLE);
        matchData.down = true;
        assertStatus(() -> accounts.deleteAccount(delete), Status.Code.UNAVAILABLE);

        assertThat(linkCount(user)).isOne();
        assertThat(accounts.getUser(GetUserRequest.newBuilder().setUserId(user.getUserId()).build()).getUser()
                .getUserId()).isEqualTo(user.getUserId());
    }

    @Test
    void aDeletedUsersLinkInProgressStopsWorking() {
        User user = newUser("Sam");
        String state = startLink(user);
        rso.codes.put("code-1", new RiotAccount("puuid-sam", "Sam", "NA1", "na"));

        accounts.deleteAccount(DeleteAccountRequest.newBuilder().setUserId(user.getUserId()).build());

        assertStatus(() -> accounts.completeRiotLink(complete(user, "code-1", state)), Status.Code.INVALID_ARGUMENT);
    }

    // ---------- FinishOnboarding ----------

    @Test
    void aNewUserHasNotFinishedTheTutorial() {
        User user = newUser("Sam");

        assertThat(user.hasOnboardedAt()).isFalse();
        assertThat(accounts.getUser(GetUserRequest.newBuilder().setUserId(user.getUserId()).build())
                        .getUser()
                        .hasOnboardedAt())
                .isFalse();
    }

    @Test
    void finishingTheTutorialKeepsTheFirstTime() {
        User user = newUser("Sam");

        User finished = accounts.finishOnboarding(finish(user)).getUser();
        clock.advance(Duration.ofHours(1));
        User again = accounts.finishOnboarding(finish(user)).getUser();

        assertThat(finished.getOnboardedAt().getSeconds()).isEqualTo(FakesConfig.NOW.getEpochSecond());
        assertThat(again.getOnboardedAt()).isEqualTo(finished.getOnboardedAt());
        assertThat(accounts.getUser(GetUserRequest.newBuilder().setUserId(user.getUserId()).build())
                        .getUser()
                        .getOnboardedAt())
                .isEqualTo(finished.getOnboardedAt());
    }

    @Test
    void finishingTheTutorialKeepsTheRiotLink() {
        User user = linkedUser("Sam", "puuid-1");

        User finished = accounts.finishOnboarding(finish(user)).getUser();

        assertThat(finished.getRiotLink().getGameName()).isEqualTo("Sam");
    }

    @Test
    void finishingTheTutorialForAnUnknownUserIsNotFound() {
        assertStatus(
                () -> accounts.finishOnboarding(FinishOnboardingRequest.newBuilder()
                        .setUserId(UUID.randomUUID().toString())
                        .build()),
                Status.Code.NOT_FOUND);
    }

    // ---------- Helpers ----------

    private static FinishOnboardingRequest finish(User user) {
        return FinishOnboardingRequest.newBuilder().setUserId(user.getUserId()).build();
    }

    private User newUser(String name) {
        return accounts.ensureUser(EnsureUserRequest.newBuilder()
                        .setAuthSubject("auth0|" + name + "-" + UUID.randomUUID())
                        .setEmail(name.toLowerCase() + "@example.test")
                        .setSuggestedDisplayName(name)
                        .build())
                .getUser();
    }

    private User linkedUser(String name, String puuid) {
        User user = newUser(name);
        String code = "code-" + UUID.randomUUID();
        rso.codes.put(code, new RiotAccount(puuid, name, "NA1", "na"));
        accounts.completeRiotLink(complete(user, code, startLink(user)));
        return user;
    }

    private String startLink(User user) {
        return FakeRsoClient.stateIn(accounts.startRiotLink(StartRiotLinkRequest.newBuilder()
                .setUserId(user.getUserId()).setRedirectUri(CALLBACK).build()).getAuthorizeUrl());
    }

    private static CompleteRiotLinkRequest complete(User user, String code, String state) {
        return CompleteRiotLinkRequest.newBuilder()
                .setUserId(user.getUserId()).setCode(code).setState(state).setRedirectUri(CALLBACK).build();
    }

    private static UpdateLookRequest lookOf(String userId, String icon, String colour, String expression) {
        return UpdateLookRequest.newBuilder()
                .setUserId(userId)
                .setLook(Look.newBuilder().setIcon(icon).setColour(colour).setExpression(expression))
                .build();
    }

    private static UpdateDisplayNameRequest rename(String userId, String name) {
        return UpdateDisplayNameRequest.newBuilder().setUserId(userId).setDisplayName(name).build();
    }

    private List<UserDataStatus> statusesOf(String... userIds) {
        return accounts.getDataStatus(GetDataStatusRequest.newBuilder().addAllUserIds(List.of(userIds)).build())
                .getStatusesList();
    }

    private PlayerSyncStatus syncStatus(SyncStatus status, Duration ago) {
        Instant at = clock.instant().minus(ago);
        return PlayerSyncStatus.newBuilder()
                .setStatus(status)
                .setMatchesStored(41)
                .setLastSyncedAt(Timestamp.newBuilder().setSeconds(at.getEpochSecond()))
                .build();
    }

    private String subjectOf(User user) {
        return jdbc.queryForObject("SELECT auth_subject FROM account.users WHERE user_id = ?", String.class, uuid(user));
    }

    private String puuidOf(User user) {
        return jdbc.queryForObject("SELECT puuid FROM account.riot_links WHERE user_id = ?", String.class, uuid(user));
    }

    private int linkCount(User user) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM account.riot_links WHERE user_id = ?", Integer.class, uuid(user));
    }

    private static UUID uuid(User user) {
        return UUID.fromString(user.getUserId());
    }

    private static void assertStatus(Runnable call, Status.Code code) {
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(call::run)
                .satisfies(e -> assertThat(e.getStatus().getCode()).isEqualTo(code));
    }
}
