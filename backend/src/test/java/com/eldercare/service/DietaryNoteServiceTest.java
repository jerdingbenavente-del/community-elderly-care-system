package com.eldercare.service;

import com.eldercare.common.ResultCode;
import com.eldercare.dto.DietaryNoteSaveDTO;
import com.eldercare.entity.Elder;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.ElderDietaryNoteMapper;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.impl.DietaryNoteServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DietaryNoteServiceTest {

    @Mock private ElderDietaryNoteMapper dietaryNoteMapper;
    @Mock private ElderMapper elderMapper;
    @Mock private DataPermissionService dataPermissionService;
    @Mock private OperationLogService operationLogService;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private DietaryNoteServiceImpl service;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long id, String name, List<String> roles) {
        LoginUser user = new LoginUser(id, name, "x", true, roles, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void familyOtherElder_403() {
        login(4L, "family01", List.of("FAMILY"));
        when(elderMapper.selectById(99L)).thenReturn(new Elder());
        doThrow(new BusinessException(ResultCode.FORBIDDEN, "无权访问该老人数据"))
                .when(dataPermissionService).checkFamilyAccess(4L, 99L);
        DietaryNoteSaveDTO dto = new DietaryNoteSaveDTO();
        dto.setElderId(99L);
        dto.setNote("海鲜过敏");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(dto, request));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        verify(dietaryNoteMapper, never()).insert(any(com.eldercare.entity.ElderDietaryNote.class));
    }

    @Test
    void blankNote_doesNotInsert() {
        login(4L, "family01", List.of("FAMILY"));
        when(elderMapper.selectById(18L)).thenReturn(new Elder());
        when(dietaryNoteMapper.selectAnyByElderId(18L)).thenReturn(null);
        DietaryNoteSaveDTO dto = new DietaryNoteSaveDTO();
        dto.setElderId(18L);
        dto.setNote("   ");
        var vo = service.save(dto, request);
        assertNull(vo.getNote());
        verify(dietaryNoteMapper, never()).insert(any(com.eldercare.entity.ElderDietaryNote.class));
    }

    @Test
    void staffCannotUpdate() {
        login(2L, "care01", List.of("CARE_STAFF"));
        DietaryNoteSaveDTO dto = new DietaryNoteSaveDTO();
        dto.setElderId(18L);
        dto.setNote("低盐");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(dto, request));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }
}
