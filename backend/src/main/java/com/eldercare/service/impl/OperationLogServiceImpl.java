package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.PageResult;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.OperationLogQueryDTO;
import com.eldercare.entity.SysOperationLog;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.SysOperationLogMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.DataPermissionService;
import com.eldercare.service.OperationLogService;
import com.eldercare.utils.IpUtils;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.OperationLogVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private final SysOperationLogMapper sysOperationLogMapper;
    private final DataPermissionService dataPermissionService;

    public OperationLogServiceImpl(SysOperationLogMapper sysOperationLogMapper,
                                   DataPermissionService dataPermissionService) {
        this.sysOperationLogMapper = sysOperationLogMapper;
        this.dataPermissionService = dataPermissionService;
    }

    @Override
    public void record(SysOperationLog log) {
        sysOperationLogMapper.insert(log);
    }

    @Override
    public void record(String module, String operation, String targetType, String targetId,
                       String result, HttpServletRequest request) {
        SysOperationLog log = new SysOperationLog();
        LoginUser user = SecurityUtils.getCurrentUser();
        if (user != null) {
            log.setUserId(user.getUserId());
            log.setUsername(user.getUsername());
        }
        log.setModule(module);
        log.setOperation(operation);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        if (request != null) {
            log.setRequestMethod(request.getMethod());
            log.setRequestUri(request.getRequestURI());
            log.setRequestIp(IpUtils.resolveClientIp(request));
        }
        log.setResult(result);
        sysOperationLogMapper.insert(log);
    }

    @Override
    public PageResult<OperationLogVO> page(OperationLogQueryDTO query) {
        dataPermissionService.denyFamilyOnAdminApi();
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可查看操作日志");
        }
        if (query.getDateFrom() != null && query.getDateTo() != null
                && query.getDateFrom().isAfter(query.getDateTo())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "开始日期不能晚于结束日期");
        }

        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(SysOperationLog::getUsername, query.getUsername().trim());
        }
        if (StringUtils.hasText(query.getModule())) {
            wrapper.eq(SysOperationLog::getModule, query.getModule().trim());
        }
        if (query.getDateFrom() != null) {
            wrapper.ge(SysOperationLog::getCreatedAt, query.getDateFrom().atStartOfDay());
        }
        if (query.getDateTo() != null) {
            wrapper.lt(SysOperationLog::getCreatedAt, query.getDateTo().plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(SysOperationLog::getCreatedAt).orderByDesc(SysOperationLog::getId);

        Page<SysOperationLog> page = sysOperationLogMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);
        List<OperationLogVO> records = page.getRecords().stream().map(this::toVo).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private OperationLogVO toVo(SysOperationLog row) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(row.getId());
        vo.setUserId(row.getUserId());
        vo.setUsername(row.getUsername());
        vo.setModule(row.getModule());
        vo.setOperation(row.getOperation());
        vo.setTargetType(row.getTargetType());
        vo.setTargetId(row.getTargetId());
        vo.setRequestMethod(row.getRequestMethod());
        vo.setRequestUri(row.getRequestUri());
        vo.setRequestIp(row.getRequestIp());
        vo.setResult(row.getResult());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }
}
