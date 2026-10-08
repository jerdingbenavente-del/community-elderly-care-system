package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.LoginRequest;
import com.eldercare.entity.SysOperationLog;
import com.eldercare.entity.SysUser;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.security.JwtTokenProvider;
import com.eldercare.security.LoginUser;
import com.eldercare.security.LoginUserDetailsService;
import com.eldercare.service.AuthService;
import com.eldercare.service.OperationLogService;
import com.eldercare.utils.IpUtils;
import com.eldercare.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginUserDetailsService loginUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final OperationLogService operationLogService;

    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           PasswordEncoder passwordEncoder,
                           LoginUserDetailsService loginUserDetailsService,
                           JwtTokenProvider jwtTokenProvider,
                           OperationLogService operationLogService) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.loginUserDetailsService = loginUserDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.operationLogService = operationLogService;
    }

    @Override
    public LoginVO login(LoginRequest request, HttpServletRequest httpRequest) {
        String username = request.getUsername().trim();
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            recordLoginLog(null, username, httpRequest, "FAIL");
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            recordLoginLog(user.getId(), username, httpRequest, "FAIL");
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已停用");
        }

        LoginUser loginUser = loginUserDetailsService.loadLoginUserById(user.getId());
        String token = jwtTokenProvider.createToken(
                loginUser.getUserId(),
                loginUser.getUsername(),
                loginUser.getRoles()
        );

        recordLoginLog(user.getId(), username, httpRequest, "SUCCESS");

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(loginUser.getUserId());
        vo.setUsername(loginUser.getUsername());
        vo.setRoles(loginUser.getRoles());
        vo.setPermissions(loginUser.getPermissions());
        vo.setMustChangePassword(loginUser.isMustChangePassword());
        return vo;
    }

    private void recordLoginLog(Long userId, String username, HttpServletRequest request, String result) {
        SysOperationLog log = new SysOperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setModule("auth");
        log.setOperation("LOGIN");
        log.setRequestMethod(request.getMethod());
        log.setRequestUri(request.getRequestURI());
        log.setRequestIp(IpUtils.resolveClientIp(request));
        log.setResult(result);
        operationLogService.record(log);
    }
}
