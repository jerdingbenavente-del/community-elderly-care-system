package com.eldercare.service;

import com.eldercare.dto.BusinessAccountCreateDTO;
import com.eldercare.vo.BusinessAccountVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * FAMILY / CARE_STAFF 业务开户：自动用户名、初始密码、首次改密标记、按 roleCode 赋角色。
 */
public interface BusinessAccountService {

    BusinessAccountVO create(BusinessAccountCreateDTO dto, HttpServletRequest request);
}
