package com.ngstars.idp.security;


import com.ngstars.idp.entity.Role;
import com.ngstars.idp.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wrapper User -> UserDetails utilisé par Spring Security.
 * Stocke l'id utilisateur en complément (pratique pour logs / audits).
 */
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final Set<GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.enabled = user.isEnabled();

        // Convertit Roles -> GrantedAuthority (ex: ROLE_USER -> new SimpleGrantedAuthority("ROLE_USER"))
        Set<Role> roles = user.getRoles() == null ? Collections.emptySet() : user.getRoles();
        this.authorities = roles.stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.unmodifiableSet(authorities);
    }

    @Override
    public String getPassword() {
        return password;
    }

    /**
     * On considère l'email comme le username principal pour l'authentification.
     */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        // Implémentation simple : toujours true. Adapter si besoin.
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Implémentation simple : toujours true. Si tu mets un champ lock, change ici.
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // Toujours true ici.
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
