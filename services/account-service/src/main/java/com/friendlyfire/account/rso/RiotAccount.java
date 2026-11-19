package com.friendlyfire.account.rso;

/** A Riot account as Riot Sign-On reports it. The PUUID never leaves Account and Match Data. */
public record RiotAccount(String puuid, String gameName, String tagLine, String region) {}
