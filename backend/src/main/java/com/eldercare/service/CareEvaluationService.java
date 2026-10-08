package com.eldercare.service;

import com.eldercare.common.PageResult;
import com.eldercare.dto.CareEvaluationCreateDTO;
import com.eldercare.dto.CareEvaluationQueryDTO;
import com.eldercare.vo.CareEvaluationVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface CareEvaluationService {

    CareEvaluationVO createForFamily(Long orderId, CareEvaluationCreateDTO dto, HttpServletRequest request);

    CareEvaluationVO getByOrderIdForFamily(Long orderId);

    List<CareEvaluationVO> listForFamily();

    PageResult<CareEvaluationVO> pageForAdmin(CareEvaluationQueryDTO query);

    CareEvaluationVO getByIdForAdmin(Long id);
}
