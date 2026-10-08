package com.eldercare.utils;

import com.eldercare.common.ResultCode;
import com.eldercare.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordRulesTest {

    @Test
    void rejectPureDigits() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> PasswordRules.assertValidNewPassword("123456"));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    void rejectPureLetters() {
        assertThrows(BusinessException.class, () -> PasswordRules.assertValidNewPassword("abcdef"));
        assertThrows(BusinessException.class, () -> PasswordRules.assertValidNewPassword("ABCDEF"));
    }

    @Test
    void acceptLetterAndDigit() {
        assertDoesNotThrow(() -> PasswordRules.assertValidNewPassword("abc123"));
        assertDoesNotThrow(() -> PasswordRules.assertValidNewPassword("Family123"));
        assertDoesNotThrow(() -> PasswordRules.assertValidNewPassword("Nurse2026"));
    }

    @Test
    void rejectSpace() {
        assertThrows(BusinessException.class, () -> PasswordRules.assertValidNewPassword("abc 123"));
    }

    @Test
    void confirmMismatch() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> PasswordRules.assertConfirmMatch("abc123", "abc124"));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    void confirmMatch() {
        assertDoesNotThrow(() -> PasswordRules.assertConfirmMatch("abc123", "abc123"));
    }
}
