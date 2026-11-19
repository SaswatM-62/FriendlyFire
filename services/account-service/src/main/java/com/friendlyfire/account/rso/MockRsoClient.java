package com.friendlyfire.account.rso;

import io.grpc.Status;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * RsoClient for mock-rso-service, used until Riot approves the app. It speaks the same
 * OAuth flow as Riot Sign-On: /authorize (a page in the browser), /token, then Riot's
 * account-v1 "me" endpoint. The mock also returns the player's region there.
 */
public class MockRsoClient implements RsoClient {

    static final String CLIENT_ID = "friendlyfire";

    private final RestClient mockRso;
    private final String browserUrl;

    /**
     * @param mockRso    calls from this service (MOCK_RSO_URL, inside Docker)
     * @param browserUrl the same mock as the player's browser reaches it (MOCK_RSO_BROWSER_URL)
     */
    public MockRsoClient(RestClient mockRso, String browserUrl) {
        this.mockRso = mockRso;
        this.browserUrl = browserUrl.endsWith("/") ? browserUrl.substring(0, browserUrl.length() - 1) : browserUrl;
    }

    @Override
    public String authorizeUrl(String state, String redirectUri) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("response_type", "code");
        query.put("client_id", CLIENT_ID);
        query.put("redirect_uri", redirectUri);
        query.put("scope", "openid");
        query.put("state", state);
        return browserUrl + "/authorize?" + query.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    @Override
    public RiotAccount account(String code, String redirectUri) {
        String token = token(code, redirectUri);
        AccountMe me;
        try {
            me = mockRso.get()
                    .uri("/riot/account/v1/accounts/me")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(AccountMe.class);
        } catch (RestClientException e) {
            throw unavailable(e);
        }
        if (me == null || blank(me.puuid()) || blank(me.gameName()) || blank(me.tagLine()) || blank(me.region())) {
            throw Status.UNAVAILABLE.withDescription("Riot Sign-On sent an incomplete account").asRuntimeException();
        }
        return new RiotAccount(me.puuid(), me.gameName(), me.tagLine(), me.region());
    }

    private String token(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        Map<String, Object> body;
        try {
            body = mockRso.post()
                    .uri("/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (HttpClientErrorException.BadRequest e) {
            // invalid_grant: an unknown or already used code.
            throw Status.INVALID_ARGUMENT
                    .withDescription("Riot didn't accept that sign-in. Start linking again.")
                    .asRuntimeException();
        } catch (RestClientException e) {
            throw unavailable(e);
        }
        Object token = body == null ? null : body.get("access_token");
        if (!(token instanceof String value) || value.isBlank()) {
            throw Status.UNAVAILABLE.withDescription("Riot Sign-On sent no access token").asRuntimeException();
        }
        return value;
    }

    private static RuntimeException unavailable(RestClientException cause) {
        return Status.UNAVAILABLE.withDescription("Riot Sign-On can't be reached").withCause(cause).asRuntimeException();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    /** Riot's account-v1 "me" answer, plus the mock's region. */
    record AccountMe(String puuid, String gameName, String tagLine, String region) {}
}
