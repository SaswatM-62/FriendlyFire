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

// The hidden fields come back to POST /authorize, which checks them again.
var pickerPage = template.Must(template.New("picker").Parse(`<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Mock Riot Sign-On</title>
<style>
  body { font-family: system-ui, sans-serif; max-width: 24rem; margin: 3rem auto; padding: 0 1rem; }
  button { display: block; width: 100%; margin: 0.5rem 0; padding: 0.75rem; font-size: 1rem; cursor: pointer; }
</style>
</head>
<body>
<h1>Mock Riot Sign-On</h1>
<p>FriendlyFire wants to link a Riot account. This page stands in for Riot during development: pick a test player.</p>
<form method="post" action="/authorize">
  <input type="hidden" name="redirect_uri" value="{{.RedirectURI}}">
  <input type="hidden" name="state" value="{{.State}}">
  {{- range .Players}}
  <button name="puuid" value="{{.PUUID}}">{{.GameName}}#{{.TagLine}} ({{.Region}})</button>
  {{- end}}
  <button name="deny" value="1">Deny</button>
</form>
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
	mux.HandleFunc("GET /authorize", h.showPicker)
	mux.HandleFunc("POST /authorize", h.pick)
	mux.HandleFunc("POST /token", h.token)
	mux.HandleFunc("GET /userinfo", h.userinfo)
	mux.HandleFunc("GET /riot/account/v1/accounts/me", h.accountMe)
	return mux
}

func healthz(w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	_, _ = io.WriteString(w, "ok")
}

// showPicker is the sign-on screen: the test players, and a way to refuse.
func (h *handler) showPicker(w http.ResponseWriter, r *http.Request) {
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
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	_ = pickerPage.Execute(w, struct {
		RedirectURI, State string
		Players            []Player
	}{redirectURI, query.Get("state"), h.players})
}

// pick sends the browser back with a one-time code, or with error=access_denied.
func (h *handler) pick(w http.ResponseWriter, r *http.Request) {
	redirectURI := r.PostFormValue("redirect_uri")
	if !slices.Contains(h.redirectURIs, redirectURI) {
		http.Error(w, "redirect_uri is missing or not allowed", http.StatusBadRequest)
		return
	}
	result := url.Values{}
	if r.PostFormValue("deny") != "" {
		result.Set("error", "access_denied")
	} else {
		puuid := r.PostFormValue("puuid")
		i := slices.IndexFunc(h.players, func(player Player) bool { return player.PUUID == puuid })
		if i < 0 {
			http.Error(w, "pick one of the listed players", http.StatusBadRequest)
			return
		}
		result.Set("code", h.store.issueCode(h.players[i], redirectURI))
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
		writeJSON(w, http.StatusOK, player)
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
