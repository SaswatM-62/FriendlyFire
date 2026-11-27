package com.friendlyfire.gateway.web;

import com.friendlyfire.gateway.json.LookJson;
import com.friendlyfire.gateway.json.MeJson;
import com.friendlyfire.gateway.realtime.SessionCloser;
import com.friendlyfire.gateway.security.SignedInUser;
import com.friendlyfire.gateway.security.UserResolver;
import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.FinishOnboardingRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.Look;
import com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.UpdateLookRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.User;
import com.friendlyfire.proto.friendlyfire.room.v1.RemovePlayerRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc.RoomServiceBlockingStub;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user. */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final AccountServiceBlockingStub accounts;
    private final RoomServiceBlockingStub rooms;
    private final UserResolver users;
    private final SessionCloser sockets;

    public MeController(
            AccountServiceBlockingStub accounts, RoomServiceBlockingStub rooms, UserResolver users,
            SessionCloser sockets) {
        this.accounts = accounts;
        this.rooms = rooms;
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

    /**
     * Changes the name other players see (FF-106), the favourite look (FF-519), or both.
     * 400 INVALID_ARGUMENT for an empty or too long name, a look that isn't on offer, or a
     * body with neither.
     */
    @PatchMapping
    public MeJson update(SignedInUser me, @RequestBody MeChanges body) {
        if (body.displayName() == null && body.look() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", "Send a displayName or a look.");
        }
        User user = null;
        if (body.displayName() != null) {
            user = accounts.updateDisplayName(UpdateDisplayNameRequest.newBuilder()
                            .setUserId(me.userId())
                            .setDisplayName(body.displayName())
                            .build())
                    .getUser();
        }
        if (body.look() != null) {
            LookJson look = body.look();
            user = accounts.updateLook(UpdateLookRequest.newBuilder()
                            .setUserId(me.userId())
                            .setLook(Look.newBuilder()
                                    .setIcon(text(look.icon()))
                                    .setColour(text(look.colour()))
                                    .setExpression(text(look.expression())))
                            .build())
                    .getUser();
        }
        users.update(me.subject(), user);
        return MeJson.from(user);
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    /**
     * Marks the new-player tutorial as done (finished, or skipped from the Riot step on) and
     * returns the user with {@code onboardedAt}. Calling it again keeps the first time.
     */
    @PostMapping("/onboarding")
    public MeJson finishOnboarding(SignedInUser me) {
        User user = accounts.finishOnboarding(FinishOnboardingRequest.newBuilder().setUserId(me.userId()).build())
                .getUser();
        users.update(me.subject(), user);
        return MeJson.from(user);
    }

    /**
     * Deletes the user, their Riot link and their match data (FF-111). 204 when done. It
     * takes them out of any room first (ADR-027); if Room or Account can't finish, nothing
     * more happens and the error goes back (e.g. 503), so the player can try again. A live
     * WebSocket of theirs (another tab or device) is closed: tokens are only checked when a
     * socket connects (ADR-022).
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(SignedInUser me) {
        // Account never calls Room (Room calls Account), so the Gateway does it.
        rooms.removePlayer(RemovePlayerRequest.newBuilder().setUserId(me.userId()).build());
        accounts.deleteAccount(DeleteAccountRequest.newBuilder().setUserId(me.userId()).build());
        users.evict(me.subject());
        sockets.closeUser(me.userId(), ErrorCodes.ACCOUNT_DELETED, "Your FriendlyFire account was deleted.");
    }

    /**
     * PATCH /api/me body: either or both. {@code displayName} 1–24 characters; {@code look}
     * one of the characters, colours and faces on offer (Account checks both).
     */
    public record MeChanges(String displayName, LookJson look) {
    }
}
