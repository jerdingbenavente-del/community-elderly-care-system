package com.eldercare.controller;

import com.eldercare.common.PageResult;
import com.eldercare.common.Result;
import com.eldercare.dto.CareActivityQueryDTO;
import com.eldercare.dto.CareActivitySaveDTO;
import com.eldercare.service.CareActivityService;
import com.eldercare.vo.CareActivityVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activities")
public class CareActivityController {

    private final CareActivityService careActivityService;

    public CareActivityController(CareActivityService careActivityService) {
        this.careActivityService = careActivityService;
    }

    /** 管理员：含草稿 */
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('care:activity:admin:list')")
    public Result<PageResult<CareActivityVO>> adminPage(@Valid CareActivityQueryDTO query) {
        return Result.success(careActivityService.pageAdmin(query));
    }

    /** 家属/护理员/管理员：已发布及历史（不含草稿） */
    @GetMapping
    @PreAuthorize("hasAuthority('care:activity:list')")
    public Result<PageResult<CareActivityVO>> publicPage(@Valid CareActivityQueryDTO query) {
        return Result.success(careActivityService.pagePublic(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('care:activity:view')")
    public Result<CareActivityVO> detail(@PathVariable Long id) {
        return Result.success(careActivityService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('care:activity:add')")
    public Result<Long> create(@Valid @RequestBody CareActivitySaveDTO dto, HttpServletRequest request) {
        return Result.success(careActivityService.create(dto, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('care:activity:update')")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody CareActivitySaveDTO dto,
                               HttpServletRequest request) {
        careActivityService.update(id, dto, request);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('care:activity:publish')")
    public Result<Void> publish(@PathVariable Long id, HttpServletRequest request) {
        careActivityService.publish(id, request);
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('care:activity:cancel')")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        careActivityService.cancel(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('care:activity:delete')")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        careActivityService.delete(id, request);
        return Result.success();
    }
}
