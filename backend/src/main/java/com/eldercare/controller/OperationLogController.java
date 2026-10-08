package com.eldercare.controller;

import com.eldercare.common.PageResult;
import com.eldercare.common.Result;
import com.eldercare.dto.OperationLogQueryDTO;
import com.eldercare.service.OperationLogService;
import com.eldercare.vo.OperationLogVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统操作日志查询（仅 ADMIN）。
 */
@RestController
@RequestMapping("/api/system/operation-logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('system:log:list')")
    public Result<PageResult<OperationLogVO>> page(OperationLogQueryDTO query) {
        return Result.success(operationLogService.page(query));
    }
}
