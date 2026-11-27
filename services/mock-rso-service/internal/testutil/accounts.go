// Package testutil holds mock-rso-service's shared test fakes.
package testutil

import (
	"context"
	"strings"

	"github.com/SaswatM-62/FriendlyFire/services/mock-rso-service/internal/lookup"
)

// FakeAccounts is an rso.AccountLookup over a fixed list. A Riot ID it doesn't know
// gets lookup.ErrNotFound, unless Errors has an error for it.
type FakeAccounts struct {
	// Accounts by Riot ID ("Sam#NA1"), not case-sensitive.
	Accounts map[string]lookup.Account
	Errors   map[string]error
	// Calls are the Riot IDs asked for, in order.
	Calls []string
}

// Account implements rso.AccountLookup.
func (f *FakeAccounts) Account(_ context.Context, gameName, tagLine string) (lookup.Account, error) {
	riotID := gameName + "#" + tagLine
	f.Calls = append(f.Calls, riotID)
	for id, err := range f.Errors {
		if strings.EqualFold(id, riotID) {
			return lookup.Account{}, err
		}
	}
	for id, account := range f.Accounts {
		if strings.EqualFold(id, riotID) {
			return account, nil
		}
	}
	return lookup.Account{}, lookup.ErrNotFound
}
