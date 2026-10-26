package com.friendlyfire.gateway.web;

import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user. */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final AccountServiceBlockingStub accounts;

    public MeController(AccountServiceBlockingStub accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> me() {
        // TODO(ff): return the signed-in user, creating them on their first visit
        //   - calls AccountService.EnsureUser with the Auth0 subject, email and name from
        //     the checked token (Auth0 comes later)
        //   - 200 with the user and their Riot link (game name, tag line, region)
        //   - uses: accounts
        return NotImplemented.response();
    }

    @PatchMapping
    public ResponseEntity<Map<String, String>> updateDisplayName() {
        // TODO(ff): change the signed-in user's display name
        //   - body {"displayName": "..."}; calls AccountService.UpdateDisplayName
        //   - 400 when the name is empty or longer than 24 characters (INVALID_ARGUMENT)
        //   - uses: accounts
        return NotImplemented.response();
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteAccount() {
        // TODO(ff): delete the signed-in user, their Riot link and their match data
        //   - calls AccountService.DeleteAccount; 204 when done
        //   - uses: accounts
        return NotImplemented.response();
    }
}
