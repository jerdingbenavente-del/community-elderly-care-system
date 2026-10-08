package com.eldercare.service;

import com.eldercare.common.PageResult;
import com.eldercare.dto.ChangePasswordDTO;
import com.eldercare.dto.SysUserCreateDTO;
import com.eldercare.dto.SysUserProfileUpdateDTO;
import com.eldercare.dto.SysUserQueryDTO;
import com.eldercare.dto.SysUserUpdateDTO;
import com.eldercare.dto.UserRoleUpdateDTO;
import com.eldercare.vo.RoleVO;
import com.eldercare.vo.SysUserVO;
import com.eldercare.vo.UserSummaryVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SysUserService {

    List<UserSummaryVO> listUsers();

    PageResult<SysUserVO> page(SysUserQueryDTO query);

    SysUserVO getById(Long id);

    SysUserVO me();

    /** 当前用户修改自己的姓名/手机号；CARE_STAFF 同步 care_staff.name */
    SysUserVO updateMe(SysUserProfileUpdateDTO dto, HttpServletRequest request);

    /** 当前用户上传头像，返回更新后的资料 */
    SysUserVO uploadMyAvatar(MultipartFile file, HttpServletRequest request);

    Long create(SysUserCreateDTO dto, HttpServletRequest request);

    void update(Long id, SysUserUpdateDTO dto, HttpServletRequest request);

    void enable(Long id, HttpServletRequest request);

    void disable(Long id, HttpServletRequest request);

    void delete(Long id, HttpServletRequest request);

    List<RoleVO> listRoles(Long userId);

    void updateRoles(Long id, UserRoleUpdateDTO dto, HttpServletRequest request);

    void changePassword(ChangePasswordDTO dto, HttpServletRequest request);

    void resetPassword(Long id, HttpServletRequest request);
}
