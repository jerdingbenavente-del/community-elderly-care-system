package com.eldercare.utils;

import com.eldercare.common.ResultCode;
import com.eldercare.exception.BusinessException;
import com.eldercare.security.LoginUser;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser getCurrentUser() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            return null;
        }
        return loginUser;
    }

    public static LoginUser requireLoginUser() {
        LoginUser user = getCurrentUser();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    public static Long requireUserId() {
        return requireLoginUser().getUserId();
    }
}
