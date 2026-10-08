package com.eldercare.service;

import com.eldercare.common.PageResult;
import com.eldercare.dto.HealthWarningHandleDTO;
import com.eldercare.dto.HealthWarningQueryDTO;
import com.eldercare.entity.HealthRecord;
import com.eldercare.vo.HealthWarningVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface HealthWarningService {

    void generateWarningsForRecord(HealthRecord record);

    void refreshUnhandledWarnings(HealthRecord record);

    PageResult<HealthWarningVO> page(HealthWarningQueryDTO query);

    HealthWarningVO getById(Long id);

    void handle(Long id, HealthWarningHandleDTO dto, HttpServletRequest request);

    List<HealthWarningVO> listByElderForFamily(Long elderId);

    HealthWarningVO getByIdForFamily(Long id);

    List<HealthWarningVO> listByRecordId(Long healthRecordId);
}
