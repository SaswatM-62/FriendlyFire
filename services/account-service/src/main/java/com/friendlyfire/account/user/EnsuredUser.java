package com.friendlyfire.account.user;

/** The user returned by an ensure operation and whether this call created it. */
public record EnsuredUser(AccountUser user, boolean created) {}
