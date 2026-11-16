package com.friendlyfire.account.grpc;

import com.friendlyfire.account.riotlink.LinkStateStore;
import com.friendlyfire.account.riotlink.RiotAccountLink;
import com.friendlyfire.account.riotlink.RiotLinkRepository;
import com.friendlyfire.account.user.AccountUser;
import com.friendlyfire.account.user.EnsuredUser;
import com.friendlyfire.account.user.NewUserNames;
import com.friendlyfire.account.user.UserRepository;
import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc;
import com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.GetDataStatusRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.GetDataStatusResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.GetUserResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameResponse;
import com.friendlyfire.proto.friendlyfire.matchdata.v1.MatchDataServiceGrpc.MatchDataServiceBlockingStub;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * friendlyfire.account.v1.AccountService. EnsureUser and GetUser work; the other RPCs are
 * stubs for now. When the database can't be reached, DatabaseErrorHandler answers
 * UNAVAILABLE.
 */
@Service
public class AccountGrpcService extends AccountServiceGrpc.AccountServiceImplBase {

    private final UserRepository users;
    private final NewUserNames newUserNames;
    private final RiotLinkRepository riotLinks;
    private final LinkStateStore linkStates;
    private final MatchDataServiceBlockingStub matchData;

    public AccountGrpcService(
            UserRepository users,
            NewUserNames newUserNames,
            RiotLinkRepository riotLinks,
            LinkStateStore linkStates,
            MatchDataServiceBlockingStub matchData) {
        this.users = users;
        this.newUserNames = newUserNames;
        this.riotLinks = riotLinks;
        this.linkStates = linkStates;
        this.matchData = matchData;
    }

    @Override
    public void ensureUser(EnsureUserRequest request, StreamObserver<EnsureUserResponse> responseObserver) {
        // TODO(ff): if the user has a linked Riot account and their data isn't SYNCED (see
        //   getDataStatus), ask Match Data to sync (MatchDataService.SyncPlayer); don't
        //   fail the sign-in if that call fails
        //   - uses: riotLinks, matchData
        EnsuredUser ensured = users.ensure(
                request.getAuthSubject(),
                request.getEmail(),
                newUserNames.from(request.getSuggestedDisplayName()));
        AccountUser user = ensured.user();
        responseObserver.onNext(EnsureUserResponse.newBuilder()
                .setUser(toProto(user, riotLinks.findByUserId(user.userId())))
                .setCreated(ensured.created())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        Optional<AccountUser> user = parseUserId(request.getUserId()).flatMap(users::findById);
        if (user.isEmpty()) {
            // An ID that isn't a UUID can't belong to anyone, so it's NOT_FOUND too.
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription("user not found").asRuntimeException());
            return;
        }

        UUID userId = user.get().userId();
        responseObserver.onNext(GetUserResponse.newBuilder()
                .setUser(toProto(user.get(), riotLinks.findByUserId(userId)))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateDisplayName(
            UpdateDisplayNameRequest request, StreamObserver<UpdateDisplayNameResponse> responseObserver) {
        // TODO(ff): change the name other players see and return the updated user
        //   - errors: INVALID_ARGUMENT if the name is empty or longer than 24 characters;
        //     NOT_FOUND if the user doesn't exist
        //   - uses: users
        notImplemented(responseObserver);
    }

    @Override
    public void startRiotLink(StartRiotLinkRequest request, StreamObserver<StartRiotLinkResponse> responseObserver) {
        // TODO(ff): step 1 of linking a Riot account: return the Riot Sign-On URL
        //   - create a one-time `state` (expires after 10 minutes), store it for this
        //     user, and build the authorize URL with it and redirect_uri
        //   - redirect_uri must be one of the URIs registered with Riot (or the mock)
        //   - errors: none in the contract
        //   - uses: linkStates, an RsoClient (not wired yet: MockRsoClient or RiotRsoClient)
        notImplemented(responseObserver);
    }

    @Override
    public void completeRiotLink(
            CompleteRiotLinkRequest request, StreamObserver<CompleteRiotLinkResponse> responseObserver) {
        // TODO(ff): step 2 of linking, after Riot redirects back with a code
        //   - check `state` (exists, not expired, belongs to user_id) and use it up
        //   - swap the code for a token, look up the player's PUUID and Riot ID, save the
        //     link, start a match sync (MatchDataService.SyncPlayer), then drop the token
        //   - errors: INVALID_ARGUMENT if `state` is missing, expired or another user's;
        //     ALREADY_EXISTS if this Riot account is linked to a different user;
        //     UNAVAILABLE if Riot Sign-On can't be reached
        //   - uses: linkStates, the RsoClient, riotLinks, matchData
        notImplemented(responseObserver);
    }

    @Override
    public void unlinkRiot(UnlinkRiotRequest request, StreamObserver<UnlinkRiotResponse> responseObserver) {
        // TODO(ff): remove the Riot link and delete all of that player's match data
        //   - delete the link, then call MatchDataService.DeletePlayerData
        //   - errors: none in the contract
        //   - uses: riotLinks, matchData
        notImplemented(responseObserver);
    }

    @Override
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> responseObserver) {
        // TODO(ff): delete the user, their Riot link and their match data
        //   - call MatchDataService.DeletePlayerData, then delete the link and the user
        //   - then ScoringService.AnonymizePlayer, so past quizzes show "Deleted player"
        //     (ADR-027; needs a scoring client and a scoring-service health check)
        //   - the Gateway has already called RoomService.RemovePlayer; Account never calls
        //     Room (Room calls Account, and a cycle would break the health checks)
        //   - errors: none in the contract
        //   - uses: users, riotLinks, matchData, a ScoringServiceBlockingStub
        notImplemented(responseObserver);
    }

    @Override
    public void getDataStatus(GetDataStatusRequest request, StreamObserver<GetDataStatusResponse> responseObserver) {
        // TODO(ff): each user's match data status (ADR-010: Account owns the 24-hour rule)
        //   - one MatchDataService.GetSyncStatus call for all of them, then: no Riot link ->
        //     NOT_LINKED; IN_PROGRESS -> SYNCING (with estimated_seconds); FAILED -> FAILED
        //     (with error_message); last successful sync under 24 hours ago -> SYNCED;
        //     otherwise NOT_SYNCED
        //   - pass on matches_stored and last_synced_at; unknown users -> NOT_LINKED
        //   - "24 hours ago" from an injected java.time.Clock
        //   - errors: none in the contract
        //   - uses: riotLinks, matchData, a Clock
        notImplemented(responseObserver);
    }

    private static void notImplemented(StreamObserver<?> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED.withDescription("not implemented yet").asRuntimeException());
    }

    private static Optional<UUID> parseUserId(String userId) {
        try {
            return Optional.of(UUID.fromString(userId));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    // Every RPC that returns a User passes the link, so none can leave it out by mistake.
    private static com.friendlyfire.proto.friendlyfire.account.v1.User toProto(
            AccountUser user, Optional<RiotAccountLink> riotLink) {
        var builder = com.friendlyfire.proto.friendlyfire.account.v1.User.newBuilder()
                .setUserId(user.userId().toString())
                .setEmail(user.email())
                .setDisplayName(user.displayName())
                .setCreatedAt(timestamp(user.createdAt()));
        // RiotLink has no PUUID field: the PUUID never leaves this service.
        riotLink.ifPresent(link -> builder.setRiotLink(
                com.friendlyfire.proto.friendlyfire.account.v1.RiotLink.newBuilder()
                        .setGameName(link.gameName())
                        .setTagLine(link.tagLine())
                        .setRegion(link.region())
                        .setLinkedAt(timestamp(link.linkedAt()))
                        .build()));
        return builder.build();
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
