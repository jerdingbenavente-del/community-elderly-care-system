package com.eldercare.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.AccountSecurityConstants;
import com.eldercare.common.ResultCode;
import com.eldercare.entity.SysUser;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.service.impl.UsernameGenerateServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsernameGenerateServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private UsernameGenerateServiceImpl usernameGenerateService;

    @Test
    void buildNamePrefix_zhangSan() {
        assertEquals("zs", usernameGenerateService.buildNamePrefix("张三"));
    }

    @Test
    void buildNamePrefix_wangXiaoLi() {
        assertEquals("wxl", usernameGenerateService.buildNamePrefix("王小丽"));
    }

    @Test
    void buildNamePrefix_xiaoMing() {
        assertEquals("xm", usernameGenerateService.buildNamePrefix("小明"));
    }

    @Test
    void buildNamePrefix_english() {
        assertEquals("tom", usernameGenerateService.buildNamePrefix("Tom"));
    }

    @Test
    void buildNamePrefix_stripSpecial() {
        assertEquals("zs", usernameGenerateService.buildNamePrefix("张 三!"));
    }

    @Test
    void generateUniqueUsername_formatAndLeadingZeroCapable() {
        when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        String username = usernameGenerateService.generateUniqueUsername("张三");
        assertTrue(username.matches("^zs@\\d{5}$"), username);
        String digits = username.substring(username.indexOf('@') + 1);
        assertEquals(5, digits.length());
    }

    @Test
    void generateUniqueUsername_retryOnConflictThenOk() {
        when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(1L)
                .thenReturn(0L);
        String username = usernameGenerateService.generateUniqueUsername("李华");
        assertTrue(username.matches("^lh@\\d{5}$"), username);
    }

    @Test
    void generateUniqueUsername_exhaustRetries() {
        when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> usernameGenerateService.generateUniqueUsername("陈美玲"));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
        assertEquals(AccountSecurityConstants.USERNAME_GEN_MAX_ATTEMPTS,
                AccountSecurityConstants.USERNAME_GEN_MAX_ATTEMPTS);
    }
}
