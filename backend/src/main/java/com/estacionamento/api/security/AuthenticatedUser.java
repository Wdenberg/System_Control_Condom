package com.estacionamento.api.security;

import com.estacionamento.api.domain.role.Permission;
import com.estacionamento.api.domain.role.RoleCode;
import com.estacionamento.api.domain.role.RolePermissions;
import com.estacionamento.api.domain.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final long sessionVersion;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final RoleCode role;
    private final boolean active;
    private final List<GrantedAuthority> authorities;

    private AuthenticatedUser(
            Long id,
            long sessionVersion,
            String username,
            String passwordHash,
            String fullName,
            RoleCode role,
            boolean active,
            List<GrantedAuthority> authorities
    ) {
        this.id = id;
        this.sessionVersion = sessionVersion;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
        this.authorities = List.copyOf(authorities);
    }

    public static AuthenticatedUser from(User user) {
        RoleCode role = user.getRole().getCode();
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        for (Permission permission : RolePermissions.of(role)) {
            authorities.add(new SimpleGrantedAuthority(permission.name()));
        }

        return new AuthenticatedUser(
                user.getId(),
                user.getSessionVersion(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getFullName(),
                role,
                user.isActive() && user.getRole().isActive(),
                authorities
        );
    }

    public long getSessionVersion() { return sessionVersion; }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public RoleCode getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
