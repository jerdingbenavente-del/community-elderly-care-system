package com.eldercare.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录用户主体，权限码映射为 GrantedAuthority。
 */
public class LoginUser implements UserDetails {

    private final Long userId;
    private final String username;
    private final String passwordHash;
    private final boolean enabled;
    private final boolean mustChangePassword;
    private final List<String> roles;
    private final List<String> permissions;

    public LoginUser(Long userId,
                     String username,
                     String passwordHash,
                     boolean enabled,
                     List<String> roles,
                     List<String> permissions) {
        this(userId, username, passwordHash, enabled, false, roles, permissions);
    }

    public LoginUser(Long userId,
                     String username,
                     String passwordHash,
                     boolean enabled,
                     boolean mustChangePassword,
                     List<String> roles,
                     List<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.mustChangePassword = mustChangePassword;
        this.roles = roles;
        this.permissions = permissions;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public boolean hasRole(String roleCode) {
        return roles != null && roles.contains(roleCode);
    }

    /**
     * 是否为「纯家属」角色（无 ADMIN / CARE_STAFF）。
     * <p>仅用于管理端 {@code denyFamilyOnAdminApi}：逼迫纯家属走 /api/family/**。
     * 家属数据权限（elder_family）不得依赖本方法，否则 FAMILY+CARE_STAFF 多角色会跳过校验。
     */
    public boolean isFamilyOnly() {
        return hasRole("FAMILY") && !hasRole("ADMIN") && !hasRole("CARE_STAFF");
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
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
        return enabled;
    }
}
