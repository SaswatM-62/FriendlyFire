package com.friendlyfire.gateway.web;

import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.matchdata.v1.MatchDataServiceGrpc.MatchDataServiceBlockingStub;
import java.util.Map;
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
    private final MatchDataServiceBlockingStub matchData;

    public RiotLinkController(AccountServiceBlockingStub accounts, MatchDataServiceBlockingStub matchData) {
        this.accounts = accounts;
        this.matchData = matchData;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> start() {
        // TODO(ff): start linking: return the Riot Sign-On URL to send the browser to
        //   - calls AccountService.StartRiotLink with this endpoint's callback URL
        //     (/api/link/riot/callback) as redirect_uri
        //   - 200 {"authorizeUrl": "..."}
        //   - uses: accounts
        return NotImplemented.response();
    }

    @GetMapping("/callback")
    public ResponseEntity<Map<String, String>> callback() {
        // TODO(ff): Riot redirects the browser here with ?code=...&state=...
        //   - calls AccountService.CompleteRiotLink, then redirects to the frontend
        //     (FRONTEND_ORIGIN) with the result
        //   - INVALID_ARGUMENT (bad state) and ALREADY_EXISTS (linked to someone else)
        //     become an error message on the frontend; UNAVAILABLE -> try again later
        //   - uses: accounts
        return NotImplemented.response();
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> unlink() {
        // TODO(ff): unlink the Riot account and delete its match data
        //   - calls AccountService.UnlinkRiot; 204 when done
        //   - uses: accounts
        return NotImplemented.response();
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        // TODO(ff): the "syncing your matches" screen's status
        //   - calls MatchDataService.GetSyncStatus for the signed-in user
        //   - 200 {"status": "IN_PROGRESS", "matchesStored": 12, "lastSyncedAt": ...}
        //   - uses: matchData
        return NotImplemented.response();
    }
}
