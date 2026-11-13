package com.friendlyfire.gateway.security;

import com.friendlyfire.gateway.web.ApiException;
import com.friendlyfire.proto.friendlyfire.account.v1.User;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Fills in a {@link SignedInUser} controller parameter from the checked token. */
public class SignedInUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserResolver users;

    public SignedInUserArgumentResolver(UserResolver users) {
        this.users = users;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == SignedInUser.class;
    }

    @Override
    public SignedInUser resolveArgument(
            MethodParameter parameter, ModelAndViewContainer container, NativeWebRequest request,
            WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            throw ApiException.unauthenticated();
        }
        User user = users.resolve(token.getToken());
        return new SignedInUser(user.getUserId(), user.getDisplayName(), token.getToken().getSubject());
    }
}
