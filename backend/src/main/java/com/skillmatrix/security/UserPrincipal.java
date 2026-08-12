package com.skillmatrix.security;

import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.entity.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Spring Security {@link UserDetails} adapter for {@link User}.
 * Exposes roles as {@code ROLE_<code>} and permissions as-is.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final User user;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.user = user;
        this.authorities = buildAuthorities(user);
    }

    private static List<GrantedAuthority> buildAuthorities(User user) {
        return user.getUserRoles().stream()
                .filter(UserRole::isActive)
                .flatMap(ur -> {
                    // ROLE_<code> grants from role code
                    Stream<GrantedAuthority> roleAuth = Stream.of(
                            new SimpleGrantedAuthority("ROLE_" + ur.getRole().getRoleCode()));
                    // Permission-level grants
                    Stream<GrantedAuthority> permAuth = ur.getRole().getRolePermissions().stream()
                            .filter(rp -> rp.getPermission().isActive())
                            .map(rp -> new SimpleGrantedAuthority(rp.getPermission().getPermissionCode()));
                    return Stream.concat(roleAuth, permAuth);
                })
                .distinct()
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    // UserDetails contract
    // ----------------------------------------------------------------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.isAccountLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }

    /** Convenience: public_id used as JWT subject. */
    public String getPublicId() {
        return user.getPublicId();
    }
}
