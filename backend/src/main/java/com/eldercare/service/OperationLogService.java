package com.eldercare.service;

import com.eldercare.common.PageResult;
import com.eldercare.dto.OperationLogQueryDTO;
import com.eldercare.entity.SysOperationLog;
import com.eldercare.vo.OperationLogVO;
import jakarta.servlet.http.HttpServletRequest;

public interface OperationLogService {

    void record(SysOperationLog log);

    void record(String module, String operation, String targetType, String targetId,
                String result, HttpServletRequest request);

    /** 管理端分页查询（仅 ADMIN） */
    PageResult<OperationLogVO> page(OperationLogQueryDTO query);
}
