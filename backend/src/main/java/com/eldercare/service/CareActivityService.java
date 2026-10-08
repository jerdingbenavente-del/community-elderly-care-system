package com.eldercare.service;

import com.eldercare.common.PageResult;
import com.eldercare.dto.CareActivityQueryDTO;
import com.eldercare.dto.CareActivitySaveDTO;
import com.eldercare.vo.CareActivityVO;
import jakarta.servlet.http.HttpServletRequest;

public interface CareActivityService {

    PageResult<CareActivityVO> pageAdmin(CareActivityQueryDTO query);

    PageResult<CareActivityVO> pagePublic(CareActivityQueryDTO query);

    CareActivityVO getById(Long id);

    Long create(CareActivitySaveDTO dto, HttpServletRequest request);

    void update(Long id, CareActivitySaveDTO dto, HttpServletRequest request);

    void publish(Long id, HttpServletRequest request);

    void cancel(Long id, HttpServletRequest request);

    void delete(Long id, HttpServletRequest request);
}
