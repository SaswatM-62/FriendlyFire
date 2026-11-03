package ratelimit

import (
	"net/http"
	"strconv"
	"strings"
	"time"
)

// MaxPause is the longest pause RetryAfter returns, so a strange header (or an
// overflowing number) can't stop the service for good.
const MaxPause = time.Hour

// RetryAfter reads the value of a Retry-After header, which is either a number of
// seconds or an HTTP date, and returns how long to pause from now. It returns false when
// the value is empty, negative or not understood. A date in the past gives 0.
func RetryAfter(value string, now time.Time) (time.Duration, bool) {
	value = strings.TrimSpace(value)
	if seconds, err := strconv.ParseInt(value, 10, 64); err == nil {
		if seconds < 0 {
			return 0, false
		}
		if seconds > int64(MaxPause/time.Second) {
			return MaxPause, true
		}
		return time.Duration(seconds) * time.Second, true
	}
	if date, err := http.ParseTime(value); err == nil {
		return min(max(date.Sub(now), 0), MaxPause), true
	}
	return 0, false
}
