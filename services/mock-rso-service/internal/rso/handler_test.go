package rso_test

import (
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/mock-rso-service/internal/rso"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const (
	callback          = "http://app.test/link/riot/callback"
	callbackWithQuery = "http://app.test/callback?from=mock"
)

var (
	sam  = rso.Player{PUUID: "puuid-sam", GameName: "Sam", TagLine: "NA1", Region: "na"}
	alex = rso.Player{PUUID: "puuid-alex", GameName: "Alex", TagLine: "EUW", Region: "eu"}
)

// fakeClock only moves when a test calls advance, so tests never sleep.
type fakeClock struct {
	mu  sync.Mutex
	now time.Time
}

func (c *fakeClock) Now() time.Time {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.now
}

func (c *fakeClock) advance(d time.Duration) {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.now = c.now.Add(d)
}

// mock is the service behind a real HTTP server, with a client that doesn't follow
// redirects, so tests can look at them.
type mock struct {
	t      *testing.T
	url    string
	client *http.Client
	clock  *fakeClock
}

type reply struct {
	status int
	header http.Header
	body   string
}

func newMock(t *testing.T) *mock {
	t.Helper()
	clock := &fakeClock{now: time.Date(2026, 11, 6, 12, 0, 0, 0, time.UTC)}
	server := httptest.NewServer(rso.NewHandler([]rso.Player{sam, alex}, []string{callback, callbackWithQuery}, clock.Now))
	t.Cleanup(server.Close)
	client := &http.Client{CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}
	return &mock{t: t, url: server.URL, client: client, clock: clock}
}

func (m *mock) do(request *http.Request) reply {
	m.t.Helper()
	response, err := m.client.Do(request)
	require.NoError(m.t, err)
	defer func() { _ = response.Body.Close() }()
	body, err := io.ReadAll(response.Body)
	require.NoError(m.t, err)
	return reply{status: response.StatusCode, header: response.Header, body: string(body)}
}

func (m *mock) get(path string) reply {
	m.t.Helper()
	request, err := http.NewRequest(http.MethodGet, m.url+path, nil)
	require.NoError(m.t, err)
	return m.do(request)
}

// getWith sends an Authorization header.
func (m *mock) getWith(path, authorization string) reply {
	m.t.Helper()
	request, err := http.NewRequest(http.MethodGet, m.url+path, nil)
	require.NoError(m.t, err)
	if authorization != "" {
		request.Header.Set("Authorization", authorization)
	}
	return m.do(request)
}

func (m *mock) post(path string, form url.Values) reply {
	m.t.Helper()
	request, err := http.NewRequest(http.MethodPost, m.url+path, strings.NewReader(form.Encode()))
	require.NoError(m.t, err)
	request.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	return m.do(request)
}

func authorizePath(redirectURI, state string) string {
	query := url.Values{"client_id": {"friendlyfire"}, "response_type": {"code"}, "redirect_uri": {redirectURI}, "state": {state}}
	return "/authorize?" + query.Encode()
}

// pick presses a player's button on the picker and returns where the browser is sent.
func (m *mock) pick(player rso.Player, redirectURI, state string) *url.URL {
	m.t.Helper()
	got := m.post("/authorize", url.Values{"redirect_uri": {redirectURI}, "state": {state}, "puuid": {player.PUUID}})
	require.Equal(m.t, http.StatusFound, got.status, got.body)
	location, err := url.Parse(got.header.Get("Location"))
	require.NoError(m.t, err)
	return location
}

func (m *mock) code(player rso.Player) string {
	m.t.Helper()
	code := m.pick(player, callback, "some-state").Query().Get("code")
	require.NotEmpty(m.t, code)
	return code
}

func (m *mock) swap(code, redirectURI string) reply {
	m.t.Helper()
	return m.post("/token", url.Values{"grant_type": {"authorization_code"}, "code": {code}, "redirect_uri": {redirectURI}})
}

func (m *mock) accessToken(player rso.Player) string {
	m.t.Helper()
	got := m.swap(m.code(player), callback)
	require.Equal(m.t, http.StatusOK, got.status, got.body)
	var body struct {
		AccessToken string `json:"access_token"`
	}
	require.NoError(m.t, json.Unmarshal([]byte(got.body), &body))
	require.NotEmpty(m.t, body.AccessToken)
	return body.AccessToken
}

func TestHealthzAnswersOK(t *testing.T) {
	got := newMock(t).get("/healthz")

	assert.Equal(t, http.StatusOK, got.status)
	assert.Equal(t, "ok", got.body)
}

func TestTheFullRoundTrip(t *testing.T) {
	m := newMock(t)

	// The browser opens the picker...
	page := m.get(authorizePath(callback, "state-123"))
	require.Equal(t, http.StatusOK, page.status)

	// ...the player picks Alex and is sent back with a code...
	location := m.pick(alex, callback, "state-123")
	assert.Equal(t, "state-123", location.Query().Get("state"))

	// ...account-service swaps the code for a token...
	got := m.swap(location.Query().Get("code"), callback)
	require.Equal(t, http.StatusOK, got.status, got.body)
	assert.Equal(t, "application/json", got.header.Get("Content-Type"))
	assert.Equal(t, "no-store", got.header.Get("Cache-Control"))
	var token struct {
		AccessToken string `json:"access_token"`
		TokenType   string `json:"token_type"`
		ExpiresIn   int    `json:"expires_in"`
	}
	require.NoError(t, json.Unmarshal([]byte(got.body), &token))
	assert.NotEmpty(t, token.AccessToken)
	assert.Equal(t, "Bearer", token.TokenType)
	assert.Equal(t, 3600, token.ExpiresIn)

	// ...and looks up who it belongs to.
	bearer := "Bearer " + token.AccessToken
	account := m.getWith("/riot/account/v1/accounts/me", bearer)
	assert.Equal(t, http.StatusOK, account.status)
	assert.JSONEq(t, `{"puuid": "puuid-alex", "gameName": "Alex", "tagLine": "EUW", "region": "eu"}`, account.body)
	userinfo := m.getWith("/userinfo", bearer)
	assert.Equal(t, http.StatusOK, userinfo.status)
	assert.JSONEq(t, `{"sub": "puuid-alex"}`, userinfo.body)
}

func TestEachTokenBelongsToThePlayerWhoWasPicked(t *testing.T) {
	m := newMock(t)
	samToken, alexToken := m.accessToken(sam), m.accessToken(alex)

	assert.Contains(t, m.getWith("/riot/account/v1/accounts/me", "Bearer "+samToken).body, `"puuid-sam"`)
	assert.Contains(t, m.getWith("/riot/account/v1/accounts/me", "Bearer "+alexToken).body, `"puuid-alex"`)
}

func TestThePickerListsEveryPlayerAndADenyButton(t *testing.T) {
	got := newMock(t).get(authorizePath(callback, "state-123"))

	assert.Equal(t, http.StatusOK, got.status)
	assert.Equal(t, "text/html; charset=utf-8", got.header.Get("Content-Type"))
	assert.Contains(t, got.body, "Sam#NA1 (na)")
	assert.Contains(t, got.body, "Alex#EUW (eu)")
	assert.Contains(t, got.body, `name="deny"`)
	assert.Contains(t, got.body, `value="state-123"`)
}

func TestThePickerEscapesWhatItIsGiven(t *testing.T) {
	got := newMock(t).get(authorizePath(callback, `"><script>alert(1)</script>`))

	assert.Equal(t, http.StatusOK, got.status)
	assert.NotContains(t, got.body, "<script>")
}

func TestABadRedirectURIIsRejected(t *testing.T) {
	cases := map[string]string{
		"missing":             "",
		"unknown":             "http://evil.test/link/riot/callback",
		"a longer path":       callback + "/evil",
		"an extra query":      callback + "?x=1",
		"a different scheme":  "https://app.test/link/riot/callback",
		"a trailing question": callback + "?",
	}
	for name, redirectURI := range cases {
		t.Run(name, func(t *testing.T) {
			m := newMock(t)

			page := m.get(authorizePath(redirectURI, "state-123"))
			picked := m.post("/authorize", url.Values{"redirect_uri": {redirectURI}, "puuid": {sam.PUUID}})
			denied := m.post("/authorize", url.Values{"redirect_uri": {redirectURI}, "deny": {"1"}})

			for _, got := range []reply{page, picked, denied} {
				assert.Equal(t, http.StatusBadRequest, got.status)
				assert.Empty(t, got.header.Get("Location"))
			}
		})
	}
}

func TestAResponseTypeOtherThanCodeIsRejected(t *testing.T) {
	m := newMock(t)
	for _, responseType := range []string{"", "token"} {
		query := url.Values{"redirect_uri": {callback}, "response_type": {responseType}}

		got := m.get("/authorize?" + query.Encode())

		assert.Equal(t, http.StatusBadRequest, got.status, responseType)
	}
}

func TestPickingAPlayerKeepsTheRedirectURIsOwnQuery(t *testing.T) {
	location := newMock(t).pick(sam, callbackWithQuery, "state-123")

	assert.Equal(t, "mock", location.Query().Get("from"))
	assert.NotEmpty(t, location.Query().Get("code"))
	assert.Equal(t, "state-123", location.Query().Get("state"))
}

func TestPickingAPlayerWithoutAStateLeavesItOut(t *testing.T) {
	location := newMock(t).pick(sam, callback, "")

	assert.False(t, location.Query().Has("state"))
}

func TestPickingAnUnknownPlayerIsRejected(t *testing.T) {
	got := newMock(t).post("/authorize", url.Values{"redirect_uri": {callback}, "puuid": {"puuid-nobody"}})

	assert.Equal(t, http.StatusBadRequest, got.status)
	assert.Empty(t, got.header.Get("Location"))
}

func TestDenyingSendsTheBrowserBackWithoutACode(t *testing.T) {
	got := newMock(t).post("/authorize", url.Values{"redirect_uri": {callback}, "state": {"state-123"}, "deny": {"1"}})

	require.Equal(t, http.StatusFound, got.status)
	location, err := url.Parse(got.header.Get("Location"))
	require.NoError(t, err)
	assert.Equal(t, "app.test", location.Host)
	assert.Equal(t, "/link/riot/callback", location.Path)
	assert.Equal(t, "access_denied", location.Query().Get("error"))
	assert.Equal(t, "state-123", location.Query().Get("state"))
	assert.False(t, location.Query().Has("code"))
}

func TestAReusedCodeIsInvalidGrant(t *testing.T) {
	m := newMock(t)
	code := m.code(sam)
	require.Equal(t, http.StatusOK, m.swap(code, callback).status)

	got := m.swap(code, callback)

	assert.Equal(t, http.StatusBadRequest, got.status)
	assert.JSONEq(t, `{"error": "invalid_grant"}`, got.body)
}

func TestAnUnknownCodeIsInvalidGrant(t *testing.T) {
	got := newMock(t).swap("no-such-code", callback)

	assert.Equal(t, http.StatusBadRequest, got.status)
	assert.JSONEq(t, `{"error": "invalid_grant"}`, got.body)
}

func TestACodeOnlyWorksWithItsOwnRedirectURI(t *testing.T) {
	m := newMock(t)

	got := m.swap(m.code(sam), callbackWithQuery)

	assert.Equal(t, http.StatusBadRequest, got.status)
	assert.JSONEq(t, `{"error": "invalid_grant"}`, got.body)
}

func TestACodeExpires(t *testing.T) {
	m := newMock(t)
	stillGood, tooLate := m.code(sam), m.code(alex)

	m.clock.advance(10*time.Minute - time.Second)
	assert.Equal(t, http.StatusOK, m.swap(stillGood, callback).status)

	m.clock.advance(time.Second)
	got := m.swap(tooLate, callback)
	assert.Equal(t, http.StatusBadRequest, got.status)
	assert.JSONEq(t, `{"error": "invalid_grant"}`, got.body)
}

func TestACodeOnlyWorksOnceEvenWhenRedeemedAtTheSameTime(t *testing.T) {
	m := newMock(t)
	code := m.code(sam)
	var succeeded atomic.Int32
	var callers sync.WaitGroup

	for range 20 {
		callers.Go(func() {
			form := url.Values{"grant_type": {"authorization_code"}, "code": {code}, "redirect_uri": {callback}}
			response, err := http.PostForm(m.url+"/token", form)
			if !assert.NoError(t, err) {
				return
			}
			defer func() { _ = response.Body.Close() }()
			if response.StatusCode == http.StatusOK {
				succeeded.Add(1)
			}
		})
	}
	callers.Wait()

	assert.EqualValues(t, 1, succeeded.Load())
}

func TestAnotherGrantTypeIsRejected(t *testing.T) {
	m := newMock(t)

	got := m.post("/token", url.Values{"grant_type": {"refresh_token"}, "code": {m.code(sam)}, "redirect_uri": {callback}})

	assert.Equal(t, http.StatusBadRequest, got.status)
	assert.JSONEq(t, `{"error": "unsupported_grant_type"}`, got.body)
}

func TestProtectedRoutesAnswer401WithoutAValidToken(t *testing.T) {
	m := newMock(t)
	token := m.accessToken(sam)
	code := m.code(sam)
	cases := map[string]string{
		"no header":          "",
		"an unknown token":   "Bearer no-such-token",
		"an empty token":     "Bearer ",
		"another scheme":     "Basic " + token,
		"a code, not token":  "Bearer " + code,
		"no scheme at all":   token,
		"a different header": "Token " + token,
	}
	for _, path := range []string{"/userinfo", "/riot/account/v1/accounts/me"} {
		for name, authorization := range cases {
			t.Run(path+"/"+name, func(t *testing.T) {
				got := m.getWith(path, authorization)

				assert.Equal(t, http.StatusUnauthorized, got.status)
				assert.Equal(t, "Bearer", got.header.Get("WWW-Authenticate"))
				assert.JSONEq(t, `{"error": "invalid_token"}`, got.body)
			})
		}
	}
}

func TestTheBearerSchemeIsCaseInsensitive(t *testing.T) {
	m := newMock(t)

	got := m.getWith("/userinfo", "bearer "+m.accessToken(sam))

	assert.Equal(t, http.StatusOK, got.status)
}

func TestATokenExpires(t *testing.T) {
	m := newMock(t)
	token := m.accessToken(sam)

	m.clock.advance(time.Hour - time.Second)
	assert.Equal(t, http.StatusOK, m.getWith("/userinfo", "Bearer "+token).status)

	m.clock.advance(time.Second)
	assert.Equal(t, http.StatusUnauthorized, m.getWith("/userinfo", "Bearer "+token).status)
}
