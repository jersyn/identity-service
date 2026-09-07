package com.identity.service.service;

import com.identity.service.security.HumanUserPrincipal;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangePasswordService {

    private final PasswordCredentialService passwordCredentialService;

    public ChangePasswordService(PasswordCredentialService passwordCredentialService) {
        this.passwordCredentialService = passwordCredentialService;
    }

    @Transactional
    public void changePassword(HumanUserPrincipal principal,
                               CharSequence currentPassword,
                               CharSequence newPassword) {
        if (!principal.isEnabled()) {
            throw new BadCredentialsException("Bad credentials");
        }

        var userId = principal.getUser().getId();

        if (!passwordCredentialService.matches(userId, currentPassword)) {
            throw new BadCredentialsException("Bad credentials");
        }

        passwordCredentialService.updatePassword(userId, newPassword);
    }
}
