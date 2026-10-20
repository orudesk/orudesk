package com.orudesk.orudesk.identity.adapter.in.security;

import com.orudesk.orudesk.identity.application.port.out.LoadUserPort;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.RoleCode;
import com.orudesk.orudesk.identity.domain.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Tells Spring Security how to find a user by the name typed at login (our email),
 * and translates our User into the UserDetails object Spring Security understands.
 */
@Component
class IdentityUserDetailsService implements UserDetailsService {

    private final LoadUserPort loadUserPort;

    IdentityUserDetailsService(LoadUserPort loadUserPort) {
        this.loadUserPort = loadUserPort;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user;
        try {
            user = loadUserPort.findByEmail(new Email(username))
                    .orElseThrow(() -> new UsernameNotFoundException("no such user"));
        } catch (IllegalArgumentException notAnEmail) {
            throw new UsernameNotFoundException("no such user");
        }
        return org.springframework.security.core.userdetails.User
                .withUsername(user.email().value())
                .password(user.passwordHash().value())
                .roles(user.roles().stream().map(RoleCode::value).toArray(String[]::new))
                .disabled(!user.isEnabled())
                .build();
    }
}
