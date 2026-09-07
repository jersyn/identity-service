package com.identity.service.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public class PasswordAuthentication extends AbstractAuthenticationToken {

    private final Object principal;
    private final Object credentials;

    private PasswordAuthentication(Object principal, Object credentials,
                                   Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
    }

    public static PasswordAuthentication ofCredentials(String email, CharSequence rawPassword) {
        return new PasswordAuthentication(email, rawPassword, Collections.emptyList());
    }

    public PasswordAuthentication(HumanUserPrincipal principal) {
        super(principal.getAuthorities());
        this.principal = principal;
        this.credentials = null;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        if (principal instanceof HumanUserPrincipal humanUserPrincipal) {
            return humanUserPrincipal.getUsername();
        }
        return super.getName();
    }
}
