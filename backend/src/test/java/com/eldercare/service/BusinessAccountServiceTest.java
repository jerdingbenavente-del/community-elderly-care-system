package com.eldercare.service;

import com.eldercare.common.AccountSecurityConstants;
import com.eldercare.common.ResultCode;
import com.eldercare.common.UserStatuses;
import com.eldercare.dto.BusinessAccountCreateDTO;
import com.eldercare.entity.SysRole;
import com.eldercare.entity.SysUser;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.SysRoleMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.mapper.SysUserRoleMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.BusinessAccountServiceImpl;
import com.eldercare.vo.BusinessAccountVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessAccountServiceTest {

    @Mock private SysUserMapper sysUserMapper;
    @Mock private SysRoleMapper sysRoleMapper;
    @Mock private SysUserRoleMapper sysUserRoleMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UsernameGenerateService usernameGenerateService;
    @Mock private DataPermissionService dataPermissionService;
    @Mock private OperationLogService operationLogService;

    @InjectMocks
    private BusinessAccountServiceImpl businessAccountService;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void loginAdmin() {
        LoginUser user = new LoginUser(1L, "admin", "x", true, List.of("ADMIN"), List.of("system:user:add"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void createFamily_ok_bcryptAndMustChange() {
        loginAdmin();
        SysRole role = new SysRole();
        role.setId(3L);
        role.setRoleCode("FAMILY");
        role.setStatus(1);
        when(sysRoleMapper.selectOne(any())).thenReturn(role);
        when(usernameGenerateService.generateUniqueUsername("王小丽")).thenReturn("wxl@48219");
        when(passwordEncoder.encode(AccountSecurityConstants.INITIAL_PASSWORD)).thenReturn("BCRYPT_HASH");
        doAnswer(inv -> {
            SysUser u = inv.getArgument(0);
            u.setId(99L);
            return 1;
        }).when(sysUserMapper).insert(any(SysUser.class));

        BusinessAccountCreateDTO dto = new BusinessAccountCreateDTO();
        dto.setName("王小丽");
        dto.setRoleCode("FAMILY");

        BusinessAccountVO vo = businessAccountService.create(dto, null);
        assertEquals(99L, vo.getUserId());
        assertEquals("wxl@48219", vo.getUsername());
        assertEquals("FAMILY", vo.getRoleCode());
        assertTrue(vo.getMustChangePassword());

        ArgumentCaptor<SysUser> cap = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(cap.capture());
        assertEquals("BCRYPT_HASH", cap.getValue().getPasswordHash());
        assertNotEquals(AccountSecurityConstants.INITIAL_PASSWORD, cap.getValue().getPasswordHash());
        assertTrue(cap.getValue().mustChangePassword());
        assertEquals(UserStatuses.ENABLED, cap.getValue().getStatus());
    }

    @Test
    void create_rejectAdminRole() {
        loginAdmin();
        BusinessAccountCreateDTO dto = new BusinessAccountCreateDTO();
        dto.setName("张三");
        dto.setRoleCode("ADMIN");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> businessAccountService.create(dto, null));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }
}
