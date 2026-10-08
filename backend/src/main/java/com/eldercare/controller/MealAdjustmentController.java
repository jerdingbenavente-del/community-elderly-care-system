package com.eldercare.controller;

import com.eldercare.common.Result;
import com.eldercare.dto.MealAdjustmentSaveDTO;
import com.eldercare.service.MealAdjustmentService;
import com.eldercare.vo.MealAdjustmentVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meal-adjustments")
public class MealAdjustmentController {

    private final MealAdjustmentService mealAdjustmentService;

    public MealAdjustmentController(MealAdjustmentService mealAdjustmentService) {
        this.mealAdjustmentService = mealAdjustmentService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('care:meal-adjust:add')")
    public Result<MealAdjustmentVO> save(@Valid @RequestBody MealAdjustmentSaveDTO dto, HttpServletRequest request) {
        return Result.success(mealAdjustmentService.save(dto, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('care:meal-adjust:delete')")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        mealAdjustmentService.cancel(id, request);
        return Result.success();
    }
}
