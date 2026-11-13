package com.friendlyfire.gateway.web;

import com.friendlyfire.gateway.json.MeJson;
import com.friendlyfire.gateway.realtime.SessionCloser;
import com.friendlyfire.gateway.security.SignedInUser;
import com.friendlyfire.gateway.security.UserResolver;
import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user. */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final AccountServiceBlockingStub accounts;
    private final UserResolver users;
    private final SessionCloser sockets;

    public MeController(AccountServiceBlockingStub accounts, UserResolver users, SessionCloser sockets) {
        this.accounts = accounts;
        this.users = users;
        this.sockets = sockets;
    }

    /**
     * The signed-in user and their Riot link, creating them on their first visit (FF-103).
     * The frontend calls this on every sign-in and app load, so it always asks Account,
     * which also starts a sync if the player's match data is stale.
     */
    @GetMapping
    public MeJson me(@AuthenticationPrincipal Jwt token) {
        return MeJson.from(users.refresh(token));
    }

    /** Changes the name other players see (FF-106). 400 INVALID_ARGUMENT for an empty or too long name. */
    @PatchMapping
    public MeJson updateDisplayName(SignedInUser me, @RequestBody NewDisplayName body) {
        User user = accounts.updateDisplayName(UpdateDisplayNameRequest.newBuilder()
                        .setUserId(me.userId())
                        .setDisplayName(body.displayName() == null ? "" : body.displayName())
                        .build())
                .getUser();
        users.update(me.subject(), user);
        return MeJson.from(user);
    }

    /**
     * Deletes the user, their Riot link and their match data (FF-111). 204 when done. A
     * live WebSocket of theirs (another tab or device) is closed: tokens are only checked
     * when a socket connects (ADR-022).
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(SignedInUser me) {
        // TODO(ff): take the player out of any room first (FF-111, ADR-027)
        //   - call RoomService.RemovePlayer before DeleteAccount, once FF-202 adds it
        //   - uses: a RoomServiceBlockingStub
        accounts.deleteAccount(DeleteAccountRequest.newBuilder().setUserId(me.userId()).build());
        users.evict(me.subject());
        sockets.closeUser(me.userId(), ErrorCodes.ACCOUNT_DELETED, "Your FriendlyFire account was deleted.");
    }

    /** PATCH /api/me body: 1–24 characters (Account checks). */
    public record NewDisplayName(String displayName) {
    }
}
