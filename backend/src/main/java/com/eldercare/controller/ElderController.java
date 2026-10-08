package com.eldercare.controller;

import com.eldercare.common.PageResult;
import com.eldercare.common.Result;
import com.eldercare.dto.ElderCreateDTO;
import com.eldercare.dto.ElderQueryDTO;
import com.eldercare.dto.ElderUpdateDTO;
import com.eldercare.service.ElderService;
import com.eldercare.vo.ElderVO;
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
@RequestMapping("/api/elders")
public class ElderController {

    private final ElderService elderService;

    public ElderController(ElderService elderService) {
        this.elderService = elderService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('elder:list')")
    public Result<PageResult<ElderVO>> page(@Valid ElderQueryDTO query) {
        return Result.success(elderService.page(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('elder:view')")
    public Result<ElderVO> detail(@PathVariable Long id) {
        return Result.success(elderService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('elder:add')")
    public Result<Long> create(@Valid @RequestBody ElderCreateDTO dto, HttpServletRequest request) {
        return Result.success(elderService.create(dto, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('elder:update')")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody ElderUpdateDTO dto,
                               HttpServletRequest request) {
        elderService.update(id, dto, request);
        return Result.success();
    }

    /**
     * 逻辑删除/停用老人档案，非物理删除。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('elder:delete')")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        elderService.delete(id, request);
        return Result.success();
    }
}
