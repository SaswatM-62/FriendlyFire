// Package ratelimit keeps match-data-service under HenrikDev's limit of 30 requests
// per minute.
//
// TODO(ff): a limiter every HenrikDev request waits on
//   - at most 30 requests per minute, shared by every sync
//   - wait (respecting the context) instead of failing when the budget is used up
//   - take a clock in the constructor so tests don't sleep
package ratelimit
