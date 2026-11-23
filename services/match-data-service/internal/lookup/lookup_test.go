package lookup

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestTheTablesLoadWithRolesAbilitiesAndClasses(t *testing.T) {
	tables, err := Load()
	require.NoError(t, err)

	jett, ok := tables.Agent("ADD6443A-41BD-E414-F6AD-E58D267F4E95")
	require.True(t, ok, "IDs aren't case-sensitive")
	assert.Equal(t, "Jett", jett.Name)
	assert.Equal(t, "Duelist", jett.Role)
	assert.Equal(t, "Blade Storm", jett.Abilities["ultimate"])

	warden, ok := tables.Weapon("8db0a1bf-4a50-832a-4566-faaaa6d250ca")
	require.True(t, ok)
	assert.Equal(t, "Rifle", warden.Class, "the Warden is a rifle (PM decision)")
	vandal, ok := tables.Weapon("9c82e19d-4575-0200-1a81-3eacf00cf872")
	require.True(t, ok)
	assert.Equal(t, "Rifle", vandal.Class)
	for id, gun := range AbilityGuns {
		got, ok := tables.Weapon(id)
		require.True(t, ok, gun.Name)
		assert.Equal(t, gun.AbilitySlot, got.AbilitySlot, gun.Name)
	}
	for id, agent := range tables.Agents {
		assert.Contains(t, []string{"Duelist", "Initiator", "Controller", "Sentinel"}, agent.Role, id)
		assert.Len(t, agent.Abilities, 4, agent.Name)
	}
}
