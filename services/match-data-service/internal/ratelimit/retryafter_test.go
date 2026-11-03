package ratelimit_test

import (
	"testing"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/ratelimit"
	"github.com/stretchr/testify/assert"
)

func TestRetryAfter(t *testing.T) {
	now := time.Date(2026, 11, 2, 12, 0, 0, 0, time.UTC)
	tests := []struct {
		name   string
		value  string
		want   time.Duration
		wantOK bool
	}{
		{"seconds", "120", 120 * time.Second, true},
		{"zero seconds", "0", 0, true},
		{"seconds with spaces", " 5 ", 5 * time.Second, true},
		{"a date 30 seconds ahead", "Mon, 02 Nov 2026 12:00:30 GMT", 30 * time.Second, true},
		{"a date in the past", "Mon, 02 Nov 2026 11:00:00 GMT", 0, true},
		{"a date far ahead is capped", "Mon, 02 Nov 2030 12:00:00 GMT", ratelimit.MaxPause, true},
		{"a huge number of seconds is capped", "99999999999999999", ratelimit.MaxPause, true},
		{"too many seconds for an int64", "999999999999999999999999", 0, false},
		{"negative", "-5", 0, false},
		{"empty", "", 0, false},
		{"words", "soon", 0, false},
		{"fractions", "1.5", 0, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, ok := ratelimit.RetryAfter(tt.value, now)

			assert.Equal(t, tt.wantOK, ok)
			assert.Equal(t, tt.want, got)
		})
	}
}
