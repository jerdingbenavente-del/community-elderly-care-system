package com.eldercare.controller;

import com.eldercare.common.Result;
import com.eldercare.service.CareServiceItemService;
import com.eldercare.vo.CareServiceItemVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 家属端服务项目目录（只读启用项，用于预约选项目）。
 * 权限复用 family:order:add，避免已部署库缺新权限码。
 */
@RestController
@RequestMapping("/api/family")
public class FamilyCareServiceItemController {

    private final CareServiceItemService careServiceItemService;

    public FamilyCareServiceItemController(CareServiceItemService careServiceItemService) {
        this.careServiceItemService = careServiceItemService;
    }

    @GetMapping("/service-items")
    @PreAuthorize("hasAuthority('family:order:add')")
    public Result<List<CareServiceItemVO>> listEnabled() {
        return Result.success(careServiceItemService.listEnabledForFamily());
    }
}
