package com.eldercare.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.entity.SysUser;
import com.eldercare.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoginUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    public LoginUserDetailsService(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return buildLoginUser(user);
    }

    public LoginUser loadLoginUserById(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return buildLoginUser(user);
    }

    private LoginUser buildLoginUser(SysUser user) {
        List<String> roles = sysUserMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = sysUserMapper.selectPermCodesByUserId(user.getId());
        boolean enabled = user.getStatus() != null && user.getStatus() == 1;
        return new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                enabled,
                user.mustChangePassword(),
                roles,
                permissions
        );
    }
}
