package com.friendlyfire.gateway.security;

/**
 * The player making a REST request. Declare it as a controller method parameter
 * (SignedInUserArgumentResolver fills it in); the user_id never comes from a request body.
 *
 * @param userId      our own ID, for every gRPC call
 * @param displayName as Account has it, for CreateRoom and JoinRoom
 * @param subject     the Auth0 subject, to update UserResolver's cache
 */
public record SignedInUser(String userId, String displayName, String subject) {
}
