package rso

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestExpiredCodesAndTokensAreForgotten(t *testing.T) {
	now := time.Date(2026, 11, 6, 12, 0, 0, 0, time.UTC)
	s := newStore(func() time.Time { return now })
	player := Player{PUUID: "p1"}
	s.issueCode(player, "http://app.test/callback")
	s.issueToken(player)
	assert.Len(t, s.codes, 1)
	assert.Len(t, s.tokens, 1)

	now = now.Add(tokenLifetime)
	s.issueCode(player, "http://app.test/callback")

	assert.Len(t, s.codes, 1, "only the new code is left")
	assert.Empty(t, s.tokens)
}

func TestARedeemedCodeIsForgottenEvenWhenTheRedirectURIIsWrong(t *testing.T) {
	now := time.Date(2026, 11, 6, 12, 0, 0, 0, time.UTC)
	s := newStore(func() time.Time { return now })
	code := s.issueCode(Player{PUUID: "p1"}, "http://app.test/callback")

	_, ok := s.redeemCode(code, "http://app.test/other")

	assert.False(t, ok)
	assert.Empty(t, s.codes)
}
