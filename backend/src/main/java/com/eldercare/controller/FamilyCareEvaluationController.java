package com.eldercare.controller;

import com.eldercare.common.Result;
import com.eldercare.dto.CareEvaluationCreateDTO;
import com.eldercare.service.CareEvaluationService;
import com.eldercare.vo.CareEvaluationVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 家属端服务评价：路径即 FAMILY 业务身份，Service 内强制 elder_family 校验。
 */
@RestController
@RequestMapping("/api/family")
public class FamilyCareEvaluationController {

    private final CareEvaluationService careEvaluationService;

    public FamilyCareEvaluationController(CareEvaluationService careEvaluationService) {
        this.careEvaluationService = careEvaluationService;
    }

    @PostMapping("/service-orders/{orderId}/evaluation")
    @PreAuthorize("hasAuthority('family:evaluation:add')")
    public Result<CareEvaluationVO> create(@PathVariable Long orderId,
                                           @Valid @RequestBody CareEvaluationCreateDTO dto,
                                           HttpServletRequest request) {
        return Result.success(careEvaluationService.createForFamily(orderId, dto, request));
    }

    @GetMapping("/service-orders/{orderId}/evaluation")
    @PreAuthorize("hasAuthority('family:evaluation:view')")
    public Result<CareEvaluationVO> getByOrder(@PathVariable Long orderId) {
        return Result.success(careEvaluationService.getByOrderIdForFamily(orderId));
    }

    @GetMapping("/evaluations")
    @PreAuthorize("hasAuthority('family:evaluation:list')")
    public Result<List<CareEvaluationVO>> listMine() {
        return Result.success(careEvaluationService.listForFamily());
    }
}
