package com.eldercare.controller;

import com.eldercare.common.PageResult;
import com.eldercare.common.Result;
import com.eldercare.dto.CareServiceItemCreateDTO;
import com.eldercare.dto.CareServiceItemQueryDTO;
import com.eldercare.dto.CareServiceItemUpdateDTO;
import com.eldercare.service.CareServiceItemService;
import com.eldercare.vo.CareServiceItemVO;
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
@RequestMapping("/api/care-service-items")
public class CareServiceItemController {

    private final CareServiceItemService careServiceItemService;

    public CareServiceItemController(CareServiceItemService careServiceItemService) {
        this.careServiceItemService = careServiceItemService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('care:service:list')")
    public Result<PageResult<CareServiceItemVO>> page(@Valid CareServiceItemQueryDTO query) {
        return Result.success(careServiceItemService.page(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('care:service:view')")
    public Result<CareServiceItemVO> detail(@PathVariable Long id) {
        return Result.success(careServiceItemService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('care:service:add')")
    public Result<Long> create(@Valid @RequestBody CareServiceItemCreateDTO dto, HttpServletRequest request) {
        return Result.success(careServiceItemService.create(dto, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('care:service:update')")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody CareServiceItemUpdateDTO dto,
                               HttpServletRequest request) {
        careServiceItemService.update(id, dto, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('care:service:delete')")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        careServiceItemService.delete(id, request);
        return Result.success();
    }
}
