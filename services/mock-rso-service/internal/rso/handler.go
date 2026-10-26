// Package rso is a stand-in for Riot Sign-On (RSO) until Riot approves the app.
// account-service talks to it exactly like it will talk to the real RSO.
package rso

import (
	"io"
	"net/http"
)

// NewHandler returns every route of the mock.
func NewHandler() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /healthz", healthz)
	mux.HandleFunc("GET /authorize", authorize)
	mux.HandleFunc("POST /token", token)
	mux.HandleFunc("GET /userinfo", userinfo)
	mux.HandleFunc("GET /riot/account/v1/accounts/me", accountMe)
	return mux
}

func healthz(w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	_, _ = io.WriteString(w, "ok")
}

func authorize(w http.ResponseWriter, r *http.Request) {
	// TODO(ff): the OAuth authorize step: "sign in" a test player without a login page
	//   - read client_id, redirect_uri and state; redirect (302) straight to
	//     redirect_uri?code=<one-time code>&state=<same state>
	//   - only accept redirect URIs from the mock's configuration
	//   - errors: 400 for a missing or unknown redirect_uri
	notImplemented(w)
}

func token(w http.ResponseWriter, r *http.Request) {
	// TODO(ff): swap a code from /authorize for an access token
	//   - form body: grant_type=authorization_code, code, redirect_uri
	//   - a code works once; answer {"access_token": ..., "token_type": "Bearer", ...}
	//   - errors: 400 {"error": "invalid_grant"} for an unknown or used code
	notImplemented(w)
}

func userinfo(w http.ResponseWriter, r *http.Request) {
	// TODO(ff): who the access token belongs to (OpenID Connect userinfo)
	//   - Authorization: Bearer <token> from /token; answer {"sub": ...}
	//   - errors: 401 for a missing or unknown token
	notImplemented(w)
}

func accountMe(w http.ResponseWriter, r *http.Request) {
	// TODO(ff): the signed-in player's Riot account, like Riot's account-v1 API
	//   - return a configured test player's PUUID and Riot ID:
	//     {"puuid": ..., "gameName": "Sam", "tagLine": "NA1"} (from env vars)
	//   - errors: 401 for a missing or unknown token
	notImplemented(w)
}

func notImplemented(w http.ResponseWriter) {
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	w.WriteHeader(http.StatusNotImplemented)
	_, _ = io.WriteString(w, "not implemented yet")
}
