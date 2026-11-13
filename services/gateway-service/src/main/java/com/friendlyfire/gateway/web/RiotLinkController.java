package com.friendlyfire.gateway.web;

import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Linking a Riot account (Riot Sign-On, or mock-rso-service locally). */
@RestController
@RequestMapping("/api/link/riot")
public class RiotLinkController {

    private final AccountServiceBlockingStub accounts;

    public RiotLinkController(AccountServiceBlockingStub accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/start")
    public ResponseEntity<?> start() {
        // TODO(ff): start linking: return the Riot Sign-On URL to send the browser to
        //   - calls AccountService.StartRiotLink with this endpoint's callback URL
        //     (/api/link/riot/callback) as redirect_uri
        //   - 200 {"authorizeUrl": "..."}
        //   - uses: accounts
        return NotImplemented.response();
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback() {
        // TODO(ff): Riot redirects the browser here with ?code=...&state=...
        //   - calls AccountService.CompleteRiotLink, then redirects to the frontend
        //     (FRONTEND_ORIGIN) with the result
        //   - INVALID_ARGUMENT (bad state) and ALREADY_EXISTS (linked to someone else)
        //     become an error message on the frontend; UNAVAILABLE -> try again later
        //   - uses: accounts
        return NotImplemented.response();
    }

    @DeleteMapping
    public ResponseEntity<?> unlink() {
        // TODO(ff): unlink the Riot account and delete its match data
        //   - calls AccountService.UnlinkRiot; 204 when done
        //   - uses: accounts
        return NotImplemented.response();
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
}
