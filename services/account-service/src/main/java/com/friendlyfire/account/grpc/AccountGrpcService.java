package com.friendlyfire.account.grpc;

import com.friendlyfire.account.riotlink.LinkStateStore;
import com.friendlyfire.account.riotlink.RiotAccountLink;
import com.friendlyfire.account.riotlink.RiotLinkRepository;
import com.friendlyfire.account.user.AccountUser;
import com.friendlyfire.account.user.EnsuredUser;
import com.friendlyfire.account.user.UserRepository;
import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc;
import com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse;
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

/** friendlyfire.account.v1.AccountService. Every RPC is a stub for now. */
@Service
public class AccountGrpcService extends AccountServiceGrpc.AccountServiceImplBase {

    private final UserRepository users;
    private final RiotLinkRepository riotLinks;
    private final LinkStateStore linkStates;
    private final MatchDataServiceBlockingStub matchData;

    public AccountGrpcService(
            UserRepository users,
            RiotLinkRepository riotLinks,
            LinkStateStore linkStates,
            MatchDataServiceBlockingStub matchData) {
        this.users = users;
        this.riotLinks = riotLinks;
        this.linkStates = linkStates;
        this.matchData = matchData;
    }

    @Override
    public void ensureUser(EnsureUserRequest request, StreamObserver<EnsureUserResponse> responseObserver) {
        EnsuredUser ensured = users.ensure(
                request.getAuthSubject(),
                request.getEmail(),
                request.getSuggestedDisplayName());
        responseObserver.onNext(EnsureUserResponse.newBuilder()
                .setUser(toProto(ensured.user()))
                .setCreated(ensured.created())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        final UUID userId;
        try {
            userId = UUID.fromString(request.getUserId());
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription("user_id must be a UUID").asRuntimeException());
            return;
        }

        Optional<AccountUser> user = users.findById(userId);
        if (user.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription("user not found").asRuntimeException());
            return;
        }

        Optional<RiotAccountLink> riotLink = riotLinks.findByUserId(userId);
        responseObserver.onNext(GetUserResponse.newBuilder()
                .setUser(toProto(user.get(), riotLink))
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
        //   - errors: none in the contract
        //   - uses: users, riotLinks, matchData
        notImplemented(responseObserver);
    }

    private static void notImplemented(StreamObserver<?> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED.withDescription("not implemented yet").asRuntimeException());
    }

    private static com.friendlyfire.proto.friendlyfire.account.v1.User toProto(AccountUser user) {
        return toProto(user, Optional.empty());
    }

    private static com.friendlyfire.proto.friendlyfire.account.v1.User toProto(
            AccountUser user, Optional<RiotAccountLink> riotLink) {
        Instant createdAt = user.createdAt();
        var builder = com.friendlyfire.proto.friendlyfire.account.v1.User.newBuilder()
                .setUserId(user.userId().toString())
                .setEmail(user.email())
                .setDisplayName(user.displayName())
                .setCreatedAt(Timestamp.newBuilder()
                        .setSeconds(createdAt.getEpochSecond())
                        .setNanos(createdAt.getNano())
                        .build());
        riotLink.ifPresent(link -> builder.setRiotLink(
                com.friendlyfire.proto.friendlyfire.account.v1.RiotLink.newBuilder()
                        .setGameName(link.gameName())
                        .setTagLine(link.tagLine())
                        .setRegion(link.region())
                        .setLinkedAt(Timestamp.newBuilder()
                                .setSeconds(link.linkedAt().getEpochSecond())
                                .setNanos(link.linkedAt().getNano())
                                .build())
                        .build()));
        return builder.build();
    }
}
