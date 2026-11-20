// Package rso is a stand-in for Riot Sign-On (RSO) until Riot approves the app.
// account-service talks to it exactly like it will talk to the real RSO.
package rso

import (
	"encoding/json"
	"html/template"
	"io"
	"net/http"
	"net/url"
	"slices"
	"strings"
	"time"
)

// signInPage stands in for Riot's sign-in and consent screen. The hidden fields come
// back to POST /authorize, which checks them again.
var signInPage = template.Must(template.New("sign-in").Parse(`<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Mock Riot Sign-On · FriendlyFire</title>
<style>
  :root { color-scheme: light dark; --ink: #1d1d22; --muted: #5b5b66; --line: #d9d9e0; --paper: #ffffff;
    --page: #f4f4f7; --accent: #e8543f; --note: #fff6e0; --note-line: #f0c14b; --bad: #b42318; }
  @media (prefers-color-scheme: dark) { :root { --ink: #f1f1f4; --muted: #a9a9b5; --line: #3a3a44;
    --paper: #1f1f25; --page: #141418; --note: #2c2615; --note-line: #8a6d1d; --bad: #ff8a7a; } }
  * { box-sizing: border-box; }
  body { margin: 0; padding: 32px 16px; background: var(--page); color: var(--ink);
    font: 16px/1.5 system-ui, -apple-system, "Segoe UI", sans-serif; }
  main { max-width: 30rem; margin: 0 auto; background: var(--paper); border: 1px solid var(--line);
    border-radius: 16px; padding: 24px; }
  h1 { font-size: 1.4rem; margin: 0 0 4px; }
  .muted { color: var(--muted); }
  .note { background: var(--note); border: 1px solid var(--note-line); border-radius: 12px;
    padding: 12px 14px; margin: 16px 0; font-size: 0.95rem; }
  .note strong { display: block; margin-bottom: 4px; font-weight: 700; }
  .note ul { margin: 6px 0 0; padding-left: 20px; }
  label { display: block; font-weight: 600; margin: 16px 0 6px; }
  input[type=text] { width: 100%; padding: 10px 12px; font-size: 1rem; border: 1px solid var(--line);
    border-radius: 10px; background: transparent; color: inherit; }
  .hint { font-size: 0.9rem; margin-top: 6px; }
  .error { color: var(--bad); font-weight: 600; margin: 12px 0 0; }
  .buttons { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 20px; }
  button { padding: 10px 16px; font-size: 1rem; border-radius: 10px; cursor: pointer;
    border: 1px solid var(--line); background: transparent; color: inherit; }
  button.primary { background: var(--accent); border-color: var(--accent); color: #fff; font-weight: 600; }
</style>
</head>
<body>
<main>
  <h1>Link your Riot account</h1>
  <p class="muted">FriendlyFire wants to read your recent VALORANT matches to make quiz questions.</p>

  <section class="note" aria-labelledby="mock-title">
    <strong id="mock-title">This is a mock Riot Sign-On page, for development and testing only.</strong>
    FriendlyFire is waiting for Riot to approve it, so this page stands in for Riot. It
    only accepts the test players on FriendlyFire's list, and only for the email each one
    is assigned to.
    <ul>
      <li>In the production app, Riot's own sign-in page opens here instead. You sign in with
        your Riot account and password, which FriendlyFire never sees.</li>
      <li>Riot then shows its consent screen: what FriendlyFire is asking for (your Riot ID
        and your match history) and buttons to allow or deny it.</li>
      <li>Nothing you type here is sent to Riot.</li>
    </ul>
  </section>

  <form method="post" action="/authorize">
    <input type="hidden" name="redirect_uri" value="{{.RedirectURI}}">
    <input type="hidden" name="state" value="{{.State}}">
    <input type="hidden" name="login_hint" value="{{.LoginHint}}">
    <label for="riot-id">Riot ID</label>
    <input type="text" id="riot-id" name="riot_id" value="{{.RiotID}}" placeholder="TheLaw#6391"
      autocomplete="off" autocapitalize="off" spellcheck="false" required
      aria-describedby="riot-id-hint{{if .Error}} riot-id-error{{end}}"{{if .Error}} aria-invalid="true"{{end}}>
    <p class="hint muted" id="riot-id-hint">Your name and tag, as VALORANT shows them.
      {{- if .LoginHint}} You're linking it to the FriendlyFire account {{.LoginHint}}.{{end}}</p>
    {{- if .Error}}
    <p class="error" id="riot-id-error" role="alert">{{.Error}}</p>
    {{- end}}
    <div class="buttons">
      <button class="primary" type="submit" name="allow" value="1">Sign in and allow</button>
      <button type="submit" name="deny" value="1" formnovalidate>Deny</button>
    </div>
  </form>
</main>
</body>
</html>
`))

type handler struct {
	players      []Player
	redirectURIs []string
	store        *store
}

// NewHandler returns every route of the mock. /authorize only sends the browser back to
// redirectURIs (exact match); now is time.Now outside tests.
func NewHandler(players []Player, redirectURIs []string, now func() time.Time) http.Handler {
	h := &handler{players: players, redirectURIs: redirectURIs, store: newStore(now)}
	mux := http.NewServeMux()
	mux.HandleFunc("GET /healthz", healthz)
	mux.HandleFunc("GET /authorize", h.showSignIn)
	mux.HandleFunc("POST /authorize", h.signIn)
	mux.HandleFunc("POST /token", h.token)
	mux.HandleFunc("GET /userinfo", h.userinfo)
	mux.HandleFunc("GET /riot/account/v1/accounts/me", h.accountMe)
	return mux
}

func healthz(w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	_, _ = io.WriteString(w, "ok")
}

// showSignIn is the sign-in screen: a Riot ID, and a way to refuse.
func (h *handler) showSignIn(w http.ResponseWriter, r *http.Request) {
	query := r.URL.Query()
	redirectURI := query.Get("redirect_uri")
	if !slices.Contains(h.redirectURIs, redirectURI) {
		http.Error(w, "redirect_uri is missing or not allowed", http.StatusBadRequest)
		return
	}
	if query.Get("response_type") != "code" {
		http.Error(w, "response_type must be code", http.StatusBadRequest)
		return
	}
	render(w, http.StatusOK, signInData{RedirectURI: redirectURI, State: query.Get("state"), LoginHint: query.Get("login_hint")})
}

type signInData struct {
	RedirectURI, State, LoginHint, RiotID, Error string
}

func render(w http.ResponseWriter, status int, data signInData) {
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.WriteHeader(status)
	_ = signInPage.Execute(w, data)
}

// signIn checks the Riot ID against the test players and the email it's assigned to,
// then sends the browser back with a one-time code; or with error=access_denied.
func (h *handler) signIn(w http.ResponseWriter, r *http.Request) {
	redirectURI := r.PostFormValue("redirect_uri")
	if !slices.Contains(h.redirectURIs, redirectURI) {
		http.Error(w, "redirect_uri is missing or not allowed", http.StatusBadRequest)
		return
	}
	result := url.Values{}
	if r.PostFormValue("deny") != "" {
		result.Set("error", "access_denied")
	} else {
		data := signInData{
			RedirectURI: redirectURI,
			State:       r.PostFormValue("state"),
			LoginHint:   strings.TrimSpace(r.PostFormValue("login_hint")),
			RiotID:      strings.TrimSpace(r.PostFormValue("riot_id")),
		}
		player, status, problem := h.check(data.RiotID, data.LoginHint)
		if problem != "" {
			data.Error = problem
			render(w, status, data)
			return
		}
		result.Set("code", h.store.issueCode(player, redirectURI))
	}
	if state := r.PostFormValue("state"); state != "" {
		result.Set("state", state)
	}

	target, err := url.Parse(redirectURI)
	if err != nil {
		http.Error(w, "redirect_uri is not a valid URL", http.StatusInternalServerError)
		return
	}
	query := target.Query()
	for key := range result {
		query.Set(key, result.Get(key))
	}
	target.RawQuery = query.Encode()
	http.Redirect(w, r, target.String(), http.StatusFound)
}

// check finds the test player for a Riot ID (not case-sensitive, like Riot's) and makes
// sure the FriendlyFire account linking it has the email assigned to them. The
// messages never show a player's assigned email.
func (h *handler) check(riotID, email string) (Player, int, string) {
	gameName, tagLine, ok := splitRiotID(riotID)
	if !ok {
		return Player{}, http.StatusBadRequest, "Enter your Riot ID with its tag, like TheLaw#6391."
	}
	i := slices.IndexFunc(h.players, func(p Player) bool {
		return strings.EqualFold(p.GameName, gameName) && strings.EqualFold(p.TagLine, tagLine)
	})
	if i < 0 {
		return Player{}, http.StatusBadRequest,
			"There's no test player with the Riot ID " + riotID + ". Check the spelling and the tag."
	}
	player := h.players[i]
	if player.Email == "" {
		return Player{}, http.StatusForbidden,
			player.RiotID() + " has no email assigned in the test player list, so it can't be linked yet."
	}
	if email == "" {
		return Player{}, http.StatusBadRequest,
			"FriendlyFire didn't say which account is linking. Start linking again from FriendlyFire."
	}
	if !strings.EqualFold(player.Email, email) {
		return Player{}, http.StatusForbidden,
			player.RiotID() + " is assigned to a different email than your FriendlyFire account's."
	}
	return player, http.StatusOK, ""
}

// splitRiotID splits "TheLaw#6391" at its last #.
func splitRiotID(riotID string) (string, string, bool) {
	i := strings.LastIndex(riotID, "#")
	if i < 0 {
		return "", "", false
	}
	gameName, tagLine := strings.TrimSpace(riotID[:i]), strings.TrimSpace(riotID[i+1:])
	return gameName, tagLine, gameName != "" && tagLine != ""
}

// token swaps a one-time code for an access token.
func (h *handler) token(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Cache-Control", "no-store")
	if r.PostFormValue("grant_type") != "authorization_code" {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "unsupported_grant_type"})
		return
	}
	player, ok := h.store.redeemCode(r.PostFormValue("code"), r.PostFormValue("redirect_uri"))
	if !ok {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid_grant"})
		return
	}
	writeJSON(w, http.StatusOK, struct {
		AccessToken string `json:"access_token"`
		TokenType   string `json:"token_type"`
		ExpiresIn   int    `json:"expires_in"`
	}{h.store.issueToken(player), "Bearer", int(tokenLifetime.Seconds())})
}

// userinfo says who the access token belongs to, like OpenID Connect's userinfo. On
// Riot's sign-on, sub is the player's PUUID.
func (h *handler) userinfo(w http.ResponseWriter, r *http.Request) {
	if player, ok := h.authenticate(w, r); ok {
		writeJSON(w, http.StatusOK, map[string]string{"sub": player.PUUID})
	}
}

// accountMe is the signed-in player's Riot account, like Riot's account-v1 API, plus
// their region.
func (h *handler) accountMe(w http.ResponseWriter, r *http.Request) {
	if player, ok := h.authenticate(w, r); ok {
		// Riot's fields only: the assigned email stays in the mock.
		writeJSON(w, http.StatusOK, struct {
			PUUID    string `json:"puuid"`
			GameName string `json:"gameName"`
			TagLine  string `json:"tagLine"`
			Region   string `json:"region"`
		}{player.PUUID, player.GameName, player.TagLine, player.Region})
	}
}

// authenticate finds the player for the request's bearer token, or answers 401.
func (h *handler) authenticate(w http.ResponseWriter, r *http.Request) (Player, bool) {
	scheme, token, _ := strings.Cut(r.Header.Get("Authorization"), " ")
	if strings.EqualFold(scheme, "Bearer") {
		if player, ok := h.store.playerForToken(token); ok {
			return player, true
		}
	}
	w.Header().Set("WWW-Authenticate", "Bearer")
	writeJSON(w, http.StatusUnauthorized, map[string]string{"error": "invalid_token"})
	return Player{}, false
}

func writeJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(body)
}
