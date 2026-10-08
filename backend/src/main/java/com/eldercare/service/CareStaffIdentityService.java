package com.eldercare.service;

import com.eldercare.entity.CareStaff;

/**
 * 当前登录用户 ↔ care_staff 身份映射（唯一可信路径：JWT userId = care_staff.user_id）。
 */
public interface CareStaffIdentityService {

    CareStaff getByUserId(Long userId);

    CareStaff requireByUserId(Long userId);

    CareStaff getCurrentCareStaff();

    CareStaff requireCurrentCareStaff();

    CareStaff requireEnabledCurrentCareStaff();

    void validateUserBinding(Long userId, Long excludeCareStaffId);
}
