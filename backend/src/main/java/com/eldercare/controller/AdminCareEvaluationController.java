package com.eldercare.controller;

import com.eldercare.common.PageResult;
import com.eldercare.common.Result;
import com.eldercare.dto.CareEvaluationQueryDTO;
import com.eldercare.service.CareEvaluationService;
import com.eldercare.vo.CareEvaluationVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端服务评价查询（仅查看，不提供修改/删除）。
 */
@RestController
@RequestMapping("/api/admin/evaluations")
public class AdminCareEvaluationController {

    private final CareEvaluationService careEvaluationService;

    public AdminCareEvaluationController(CareEvaluationService careEvaluationService) {
        this.careEvaluationService = careEvaluationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('care:evaluation:list')")
    public Result<PageResult<CareEvaluationVO>> page(@Valid CareEvaluationQueryDTO query) {
        return Result.success(careEvaluationService.pageForAdmin(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('care:evaluation:view')")
    public Result<CareEvaluationVO> detail(@PathVariable Long id) {
        return Result.success(careEvaluationService.getByIdForAdmin(id));
    }
}
