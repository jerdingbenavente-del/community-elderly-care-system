package com.eldercare.controller;

import com.eldercare.common.Result;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.LoginRequest;
import com.eldercare.exception.BusinessException;
import com.eldercare.security.LoginUser;
import com.eldercare.service.AuthService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.success(authService.login(request, httpRequest));
    }

    /**
     * 当前登录用户信息（用于鉴权联调，不含密码）。
     */
    @GetMapping("/me")
    public Result<LoginVO> me() {
        LoginUser loginUser = SecurityUtils.getCurrentUser();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        LoginVO vo = new LoginVO();
        vo.setUserId(loginUser.getUserId());
        vo.setUsername(loginUser.getUsername());
        vo.setRoles(loginUser.getRoles());
        vo.setPermissions(loginUser.getPermissions());
        vo.setMustChangePassword(loginUser.isMustChangePassword());
        return Result.success(vo);
    }
}
