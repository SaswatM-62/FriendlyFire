// Package lookup finds a Riot account's PUUID and region from its Riot ID through
// HenrikDev's account API, so testplayers.json doesn't need PUUIDs typed in by hand.
package lookup

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
)

// Account is what the test players file needs from a Riot account.
type Account struct {
	PUUID  string
	Region string
}

// ErrNotFound means HenrikDev knows no account with that Riot ID (a typo, or a renamed
// account).
var ErrNotFound = errors.New("no Riot account with that Riot ID")

// HenrikDev looks accounts up with HENRIKDEV_API_KEY, the key match-data-service uses.
type HenrikDev struct {
	baseURL string
	apiKey  string
	client  *http.Client
}

// NewHenrikDev takes the API's base URL (https://api.henrikdev.xyz) and key; the client
// should have a timeout.
func NewHenrikDev(baseURL, apiKey string, client *http.Client) *HenrikDev {
	return &HenrikDev{baseURL: strings.TrimRight(baseURL, "/"), apiKey: apiKey, client: client}
}

// Account returns the PUUID and region of gameName#tagLine.
func (h *HenrikDev) Account(ctx context.Context, gameName, tagLine string) (Account, error) {
	target := fmt.Sprintf("%s/valorant/v2/account/%s/%s", h.baseURL, url.PathEscape(gameName), url.PathEscape(tagLine))
	request, err := http.NewRequestWithContext(ctx, http.MethodGet, target, nil)
	if err != nil {
		return Account{}, err
	}
	request.Header.Set("Authorization", h.apiKey)
	request.Header.Set("Accept", "application/json")
	response, err := h.client.Do(request)
	if err != nil {
		return Account{}, fmt.Errorf("couldn't reach HenrikDev: %w", err)
	}
	defer func() { _ = response.Body.Close() }()

	switch response.StatusCode {
	case http.StatusOK:
	case http.StatusNotFound:
		return Account{}, ErrNotFound
	case http.StatusUnauthorized, http.StatusForbidden:
		return Account{}, fmt.Errorf("HenrikDev refused HENRIKDEV_API_KEY (HTTP %d)", response.StatusCode)
	case http.StatusTooManyRequests:
		return Account{}, errors.New("HenrikDev's rate limit (HTTP 429): restart the mock in a minute")
	default:
		return Account{}, fmt.Errorf("HenrikDev answered HTTP %d", response.StatusCode)
	}

	var body struct {
		Data struct {
			PUUID  string `json:"puuid"`
			Region string `json:"region"`
		} `json:"data"`
	}
	if err := json.NewDecoder(io.LimitReader(response.Body, 1<<20)).Decode(&body); err != nil {
		return Account{}, fmt.Errorf("HenrikDev sent an account we couldn't read: %w", err)
	}
	if body.Data.PUUID == "" {
		return Account{}, errors.New("HenrikDev sent an account without a PUUID")
	}
	return Account{PUUID: body.Data.PUUID, Region: body.Data.Region}, nil
}
