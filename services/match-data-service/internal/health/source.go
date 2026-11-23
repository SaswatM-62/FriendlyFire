package health

import (
	"context"

	healthv1 "github.com/SaswatM-62/FriendlyFire/gen/go/friendlyfire/health/v1"
)

// SourceCheck reports the match source: the fixtures are always up; HenrikDev reports the
// outcome of its last real request (see source.HenrikDevSource.Health), so the health
// tree never uses up HenrikDev's rate limit.
type SourceCheck struct {
	name   string
	health func() error
}

// NewSourceCheck checks a source by name ("henrikdev" or "fixtures") with its Health
// method; nil health means always up.
func NewSourceCheck(name string, health func() error) *SourceCheck {
	return &SourceCheck{name: name, health: health}
}

// Name is the source's name.
func (s *SourceCheck) Name() string { return s.name }

// Check returns UP, or the source's last error.
func (s *SourceCheck) Check(context.Context) (*healthv1.ComponentHealth, error) {
	if s.health != nil {
		if err := s.health(); err != nil {
			return nil, err
		}
	}
	return Up(), nil
}
