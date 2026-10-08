package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.AccountSecurityConstants;
import com.eldercare.common.ResultCode;
import com.eldercare.entity.SysUser;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.service.UsernameGenerateService;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;

@Service
public class UsernameGenerateServiceImpl implements UsernameGenerateService {

    private final SysUserMapper sysUserMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final HanyuPinyinOutputFormat pinyinFormat;

    public UsernameGenerateServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
        this.pinyinFormat = new HanyuPinyinOutputFormat();
        this.pinyinFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        this.pinyinFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }

    @Override
    public String generateUniqueUsername(String realName) {
        String prefix = buildNamePrefix(realName);
        if (!StringUtils.hasText(prefix)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "姓名无法生成有效账号前缀");
        }
        for (int i = 0; i < AccountSecurityConstants.USERNAME_GEN_MAX_ATTEMPTS; i++) {
            String candidate = prefix + "@" + randomFiveDigits();
            Long exists = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, candidate));
            if (exists == null || exists == 0) {
                return candidate;
            }
        }
        throw new BusinessException(ResultCode.CONFLICT, "账号自动生成失败，请重新尝试");
    }

    @Override
    public String buildNamePrefix(String realName) {
        if (!StringUtils.hasText(realName)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : realName.trim().toCharArray()) {
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (isChinese(c)) {
                char initial = chineseInitial(c);
                if (initial != 0) {
                    sb.append(initial);
                }
            } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(Character.toLowerCase(c));
            }
            // 其他特殊字符丢弃
        }
        return sb.toString();
    }

    private String randomFiveDigits() {
        int n = secureRandom.nextInt(100_000);
        return String.format("%05d", n);
    }

    private boolean isChinese(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A;
    }

    private char chineseInitial(char c) {
        try {
            String[] array = PinyinHelper.toHanyuPinyinStringArray(c, pinyinFormat);
            if (array != null && array.length > 0 && StringUtils.hasText(array[0])) {
                return Character.toLowerCase(array[0].charAt(0));
            }
        } catch (BadHanyuPinyinOutputFormatCombination ignored) {
            // fall through
        }
        return 0;
    }
}
