package rso_test

import (
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/SaswatM-62/FriendlyFire/services/mock-rso-service/internal/rso"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestHealthzAnswersOK(t *testing.T) {
	server := httptest.NewServer(rso.NewHandler())
	defer server.Close()

	response, err := http.Get(server.URL + "/healthz")
	require.NoError(t, err)
	defer func() { _ = response.Body.Close() }()
	body, err := io.ReadAll(response.Body)
	require.NoError(t, err)

	assert.Equal(t, http.StatusOK, response.StatusCode)
	assert.Equal(t, "ok", string(body))
}

func TestOAuthRoutesAreNotImplementedYet(t *testing.T) {
	server := httptest.NewServer(rso.NewHandler())
	defer server.Close()

	for _, route := range []struct{ method, path string }{
		{http.MethodGet, "/authorize"},
		{http.MethodPost, "/token"},
		{http.MethodGet, "/userinfo"},
		{http.MethodGet, "/riot/account/v1/accounts/me"},
	} {
		t.Run(route.path, func(t *testing.T) {
			request, err := http.NewRequest(route.method, server.URL+route.path, strings.NewReader(""))
			require.NoError(t, err)
			response, err := http.DefaultClient.Do(request)
			require.NoError(t, err)
			defer func() { _ = response.Body.Close() }()
			body, err := io.ReadAll(response.Body)
			require.NoError(t, err)

			assert.Equal(t, http.StatusNotImplemented, response.StatusCode)
			assert.Equal(t, "not implemented yet", string(body))
		})
	}
}
