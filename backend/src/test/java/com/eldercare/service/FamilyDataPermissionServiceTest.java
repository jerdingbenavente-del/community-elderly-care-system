package com.eldercare.service;

import com.eldercare.common.ResultCode;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.ElderFamilyMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.DataPermissionServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * P6.1：家属数据权限不依赖 isFamilyOnly；checkFamilyAccess 仅看 elder_family 绑定。
 */
@ExtendWith(MockitoExtension.class)
class FamilyDataPermissionServiceTest {

    @Mock
    private ElderFamilyMapper elderFamilyMapper;

    @InjectMocks
    private DataPermissionServiceImpl dataPermissionService;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId, List<String> roles) {
        LoginUser user = new LoginUser(userId, "u" + userId, "x", true, roles, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void checkFamilyAccess_familyBound_ok() {
        when(elderFamilyMapper.selectCount(any())).thenReturn(1L);
        assertDoesNotThrow(() -> dataPermissionService.checkFamilyAccess(3L, 1L));
        assertTrue(dataPermissionService.isFamilyBound(3L, 1L));
    }

    @Test
    void checkFamilyAccess_familyUnbound_shouldReject() {
        when(elderFamilyMapper.selectCount(any())).thenReturn(0L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> dataPermissionService.checkFamilyAccess(3L, 2L));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        assertEquals("无权访问该老人数据", ex.getMessage());
        assertFalse(dataPermissionService.isFamilyBound(3L, 2L));
    }

    @Test
    void checkFamilyAccess_ignoresRoles_onlyBindingMatters() {
        // 即使用户是 FAMILY+CARE_STAFF，绑定校验仍只看 elder_family
        when(elderFamilyMapper.selectCount(any())).thenReturn(0L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> dataPermissionService.checkFamilyAccess(100L, 2L));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void denyFamilyOnAdminApi_pureFamily_blocked() {
        login(3L, List.of("FAMILY"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> dataPermissionService.denyFamilyOnAdminApi());
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void denyFamilyOnAdminApi_familyCareStaff_allowed() {
        login(100L, List.of("FAMILY", "CARE_STAFF"));
        assertDoesNotThrow(() -> dataPermissionService.denyFamilyOnAdminApi());
    }

    @Test
    void isFamilyOnly_false_whenDualRole() {
        LoginUser dual = new LoginUser(100L, "dual", "x", true,
                List.of("FAMILY", "CARE_STAFF"), List.of());
        assertFalse(dual.isFamilyOnly());
        assertTrue(dual.hasRole("FAMILY"));
        assertTrue(dual.hasRole("CARE_STAFF"));
    }
}
