package migrations

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestMigrateURLUsesPgx5AndAShortConnectTimeout(t *testing.T) {
	got, err := migrateURL("postgres://postgres:secret@localhost:5432/friendlyfire?search_path=matchdata")

	require.NoError(t, err)
	assert.Equal(t,
		"pgx5://postgres:secret@localhost:5432/friendlyfire?connect_timeout=5&search_path=matchdata", got)
}

func TestMigrateURLRejectsAnInvalidURL(t *testing.T) {
	_, err := migrateURL("postgres://host:notaport/db")

	assert.Error(t, err)
}

func TestEmbeddedMigrationsHaveAnUpAndADownFile(t *testing.T) {
	for _, name := range []string{"sql/000001_init.up.sql", "sql/000001_init.down.sql"} {
		_, err := files.ReadFile(name)
		assert.NoError(t, err, name)
	}
}
