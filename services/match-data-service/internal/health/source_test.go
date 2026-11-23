package health

import (
	"context"
	"errors"
	"testing"

	healthv1 "github.com/SaswatM-62/FriendlyFire/gen/go/friendlyfire/health/v1"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestASourceCheckIsUpUnlessTheSourceSaysOtherwise(t *testing.T) {
	fixtures := NewSourceCheck("fixtures", nil)
	node, err := fixtures.Check(context.Background())
	require.NoError(t, err)
	assert.Equal(t, healthv1.ComponentStatus_COMPONENT_STATUS_UP, node.GetStatus())
	assert.Equal(t, "fixtures", fixtures.Name())

	_, err = NewSourceCheck("henrikdev", func() error { return errors.New("no API key") }).Check(context.Background())
	assert.EqualError(t, err, "no API key")
}
