package com.identity.service.security;

import com.identity.service.domain.User;
import com.identity.service.service.PasswordCredentialService;
import com.identity.service.service.UserService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class PasswordAuthenticationProvider implements AuthenticationProvider {

    private final UserService userService;
    private final PasswordCredentialService passwordCredentialService;

    public PasswordAuthenticationProvider(UserService userService,
                                          PasswordCredentialService passwordCredentialService) {
        this.userService = userService;
        this.passwordCredentialService = passwordCredentialService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        PasswordAuthentication auth = (PasswordAuthentication) authentication;
        String email = auth.getName().trim().toLowerCase();

        User user = userService.findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("Bad credentials"));

        if (!passwordCredentialService.matches(user.getId(), (CharSequence) auth.getCredentials())) {
            throw new BadCredentialsException("Bad credentials");
        }

        HumanUserPrincipal principal = new HumanUserPrincipal(user);
        if (!principal.isEnabled()) {
            throw new BadCredentialsException("Bad credentials");
        }
        return new PasswordAuthentication(principal);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PasswordAuthentication.class.isAssignableFrom(authentication);
    }
}
