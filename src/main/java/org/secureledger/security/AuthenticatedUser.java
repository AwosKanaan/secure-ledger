package org.secureledger.security;

import org.secureledger.model.User;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class AuthenticatedUser implements UserDetails, CredentialsContainer {

    private final UUID id;
    private final String email;
    private String passwordHash;

    private AuthenticatedUser(UUID id, String email, String passwordHash) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    static AuthenticatedUser withCredentials(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getPasswordHash());
    }

    static AuthenticatedUser withoutCredentials(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), null);
    }

    public UUID id() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }

    @Override
    public String toString() {
        return "AuthenticatedUser[id=" + id + "]";
    }
}
