package com.friendlyfire.gateway.testsupport;

import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc;
import com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.RiotLink;
import com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.User;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;

/**
 * A fake AccountService. By default the user for subject "auth0|maya" is "user-maya",
 * named after the token's name claim (see FakeTokens), with no Riot link.
 */
public final class FakeAccountService extends AccountServiceGrpc.AccountServiceImplBase {

    static final Timestamp CREATED_AT = Timestamp.newBuilder().setSeconds(1_762_000_000L).build();

    public final FakeCalls calls = new FakeCalls();

    public static User user(String name, String displayName) {
        return User.newBuilder()
                .setUserId(FakeTokens.userId(name))
                .setDisplayName(displayName)
                .setEmail(name + "@example.com")
                .setCreatedAt(CREATED_AT)
                .build();
    }

    public static RiotLink riotLink() {
        return RiotLink.newBuilder().setGameName("Maya").setTagLine("NA1").setRegion("na")
                .setLinkedAt(Timestamp.newBuilder().setSeconds(1_762_500_000L)).build();
    }

    @Override
    public void ensureUser(EnsureUserRequest request, StreamObserver<EnsureUserResponse> observer) {
        String name = request.getAuthSubject().substring(request.getAuthSubject().indexOf('|') + 1);
        calls.handle(request, EnsureUserResponse.newBuilder()
                .setUser(user(name, request.getSuggestedDisplayName()))
                .build(), observer);
    }

    @Override
    public void updateDisplayName(UpdateDisplayNameRequest request, StreamObserver<UpdateDisplayNameResponse> observer) {
        String name = request.getUserId().replaceFirst("^user-", "");
        calls.handle(request, UpdateDisplayNameResponse.newBuilder()
                .setUser(user(name, request.getDisplayName()))
                .build(), observer);
    }

    @Override
    public void startRiotLink(StartRiotLinkRequest request, StreamObserver<StartRiotLinkResponse> observer) {
        calls.handle(request, StartRiotLinkResponse.newBuilder()
                .setAuthorizeUrl("http://localhost:8090/authorize?state=s1&redirect_uri=" + request.getRedirectUri())
                .build(), observer);
    }

    @Override
    public void completeRiotLink(CompleteRiotLinkRequest request, StreamObserver<CompleteRiotLinkResponse> observer) {
        calls.handle(request, CompleteRiotLinkResponse.newBuilder().setRiotLink(riotLink()).build(), observer);
    }

    @Override
    public void unlinkRiot(UnlinkRiotRequest request, StreamObserver<UnlinkRiotResponse> observer) {
        calls.handle(request, UnlinkRiotResponse.getDefaultInstance(), observer);
    }

    @Override
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> observer) {
        calls.handle(request, DeleteAccountResponse.getDefaultInstance(), observer);
    }
}
