package com.eldercare.service;

import com.eldercare.dto.MealAdjustmentSaveDTO;
import com.eldercare.vo.MealAdjustmentVO;
import jakarta.servlet.http.HttpServletRequest;

public interface MealAdjustmentService {

    MealAdjustmentVO save(MealAdjustmentSaveDTO dto, HttpServletRequest request);

    void cancel(Long id, HttpServletRequest request);
}
