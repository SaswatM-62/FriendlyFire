package rso

import (
	"crypto/rand"
	"maps"
	"sync"
	"time"
)

const (
	codeLifetime  = 10 * time.Minute
	tokenLifetime = time.Hour
)

// store keeps one-time codes and access tokens in memory. They're short-lived, and
// losing them in a restart only means retrying a link.
type store struct {
	now func() time.Time

	mu     sync.Mutex
	codes  map[string]issuedCode
	tokens map[string]issuedToken
}

type issuedCode struct {
	player      Player
	redirectURI string
	expiresAt   time.Time
}

type issuedToken struct {
	player    Player
	expiresAt time.Time
}

func newStore(now func() time.Time) *store {
	return &store{now: now, codes: map[string]issuedCode{}, tokens: map[string]issuedToken{}}
}

// issueCode returns a new one-time code for player, which only works with redirectURI.
func (s *store) issueCode(player Player, redirectURI string) string {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.dropExpired()
	code := rand.Text()
	s.codes[code] = issuedCode{player: player, redirectURI: redirectURI, expiresAt: s.now().Add(codeLifetime)}
	return code
}

// redeemCode uses up a code, even when it fails, and returns the player it was for. It
// fails for an unknown, used or expired code, or a different redirectURI than the code's.
func (s *store) redeemCode(code, redirectURI string) (Player, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	issued, ok := s.codes[code]
	delete(s.codes, code)
	if !ok || issued.redirectURI != redirectURI || !s.now().Before(issued.expiresAt) {
		return Player{}, false
	}
	return issued.player, true
}

// issueToken returns a new access token for a player.
func (s *store) issueToken(player Player) string {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.dropExpired()
	token := rand.Text()
	s.tokens[token] = issuedToken{player: player, expiresAt: s.now().Add(tokenLifetime)}
	return token
}

// playerForToken returns who an unexpired access token belongs to.
func (s *store) playerForToken(token string) (Player, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	issued, ok := s.tokens[token]
	if !ok || !s.now().Before(issued.expiresAt) {
		return Player{}, false
	}
	return issued.player, true
}

// dropExpired keeps the maps from growing forever. The caller holds s.mu.
func (s *store) dropExpired() {
	now := s.now()
	maps.DeleteFunc(s.codes, func(_ string, code issuedCode) bool { return !now.Before(code.expiresAt) })
	maps.DeleteFunc(s.tokens, func(_ string, token issuedToken) bool { return !now.Before(token.expiresAt) })
}
