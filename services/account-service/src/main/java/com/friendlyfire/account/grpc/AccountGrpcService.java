package com.friendlyfire.account.grpc;

import com.friendlyfire.account.riotlink.LinkStateStore;
import com.friendlyfire.account.riotlink.RiotLinkRepository;
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
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
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
        // TODO(ff): find the user for an Auth0 subject, or create them on their first visit
        //   - look up by auth_subject; if there's none, create a user with a new UUID, the
        //     email and suggested_display_name from the request, and set created = true
        //   - if the user has a linked Riot account, ask Match Data to fetch new matches
        //     (MatchDataService.SyncPlayer); don't fail the sign-in if that call fails
        //   - errors: none in the contract
        //   - uses: users, riotLinks, matchData
        notImplemented(responseObserver);
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        // TODO(ff): return one user, with their Riot link if they have one
        //   - the RiotLink message has no PUUID on purpose: never send it out
        //   - errors: NOT_FOUND if the user doesn't exist
        //   - uses: users, riotLinks
        notImplemented(responseObserver);
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
}
