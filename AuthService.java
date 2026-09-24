package com.eldercare.service;

import com.eldercare.dto.LoginRequest;
import com.eldercare.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    LoginVO login(LoginRequest request, HttpServletRequest httpRequest);
}
