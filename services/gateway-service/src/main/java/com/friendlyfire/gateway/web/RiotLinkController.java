package com.friendlyfire.gateway.web;

import com.friendlyfire.gateway.json.RiotLinkJson;
import com.friendlyfire.gateway.protection.RateLimit;
import com.friendlyfire.gateway.protection.RateLimiter;
import com.friendlyfire.gateway.security.SignedInUser;
import com.friendlyfire.gateway.security.UserResolver;
import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest;
import io.grpc.Status;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Linking a Riot account (Riot Sign-On, or mock-rso-service locally). Riot sends the
 * browser back to the frontend's /link/riot/callback page, which finishes the link here
 * with the player's token: a redirect alone carries no sign-in (ADR-007).
 */
@RestController
@RequestMapping("/api/link/riot")
public class RiotLinkController {

    /** Where Riot sends the browser back to. Must be registered with Riot (or the mock). */
    static final String CALLBACK_PATH = "/link/riot/callback";

    private final AccountServiceBlockingStub accounts;
    private final UserResolver users;
    private final RateLimiter rateLimiter;
    private final String redirectUri;

    public RiotLinkController(
            AccountServiceBlockingStub accounts, UserResolver users, RateLimiter rateLimiter,
            @Value("${friendlyfire.frontend-origin}") String frontendOrigin) {
        this.accounts = accounts;
        this.users = users;
        this.rateLimiter = rateLimiter;
        this.redirectUri = frontendOrigin + CALLBACK_PATH;
    }

    /** Step 1 (FF-109): the Riot Sign-On URL to send the browser to. */
    @PostMapping("/start")
    public StartedRiotLink start(SignedInUser me, HttpServletRequest request) {
        rateLimiter.check(RateLimit.START_RIOT_LINK, me.userId(), request.getRemoteAddr());
        String url = accounts.startRiotLink(StartRiotLinkRequest.newBuilder()
                        .setUserId(me.userId())
                        .setRedirectUri(redirectUri)
                        .build())
                .getAuthorizeUrl();
        return new StartedRiotLink(url);
    }

    /**
     * Step 2 (FF-110): the code and state Riot sent back. 400 INVALID_LINK_STATE if the
     * state is missing, expired or someone else's; 409 RIOT_ALREADY_LINKED if this Riot
     * account belongs to another player; 503 if Riot Sign-On can't be reached.
     */
    @PostMapping("/complete")
    public RiotLinkJson complete(SignedInUser me, @RequestBody RiotCallback body) {
        CompleteRiotLinkResponse response = GrpcErrors.renaming(
                Map.of(Status.Code.INVALID_ARGUMENT, ErrorCodes.INVALID_LINK_STATE,
                        Status.Code.ALREADY_EXISTS, ErrorCodes.RIOT_ALREADY_LINKED),
                () -> accounts.completeRiotLink(CompleteRiotLinkRequest.newBuilder()
                        .setUserId(me.userId())
                        .setCode(body.code() == null ? "" : body.code())
                        .setState(body.state() == null ? "" : body.state())
                        .setRedirectUri(redirectUri)
                        .build()));
        users.evict(me.subject());
        return RiotLinkJson.from(response.getRiotLink());
    }

    /** Removes the Riot link and that player's match data (FF-111). 204 when done. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(SignedInUser me) {
        accounts.unlinkRiot(UnlinkRiotRequest.newBuilder().setUserId(me.userId()).build());
        users.evict(me.subject());
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        // TODO(ff): the sync banner's status (FF-112)
        //   - calls Account's sync-status RPC for the signed-in user (added by FF-202;
        //     Account owns the 24-hour rule, ADR-010), never Match Data
        //   - 200 {"status": "SYNCING", "estimatedSeconds": 120, "lastSyncedAt": ...,
        //     "matchesStored": 41, "errorMessage": ...} (SyncStatusResponse in
        //     frontend/src/lib/api-types.ts)
        //   - uses: accounts
        return NotImplemented.response();
    }

    /** POST /start's answer. */
    public record StartedRiotLink(String authorizeUrl) {
    }

    /** POST /complete body: what Riot put on the callback URL. */
    public record RiotCallback(String code, String state) {
    }
}
