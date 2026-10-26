package com.friendlyfire.account.rso;

public class MockRsoClient implements RsoClient {
    // TODO(ff): RsoClient for mock-rso-service (MOCK_RSO_URL), used in local development
    //   - uses: the injected mockRsoRestClient (HttpClientsConfig)
    //   - errors: UNAVAILABLE when the mock can't be reached
}
