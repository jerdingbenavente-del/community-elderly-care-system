package com.eldercare.controller;

import com.eldercare.common.Result;
import com.eldercare.dto.ElderFamilyCreateDTO;
import com.eldercare.service.ElderFamilyService;
import com.eldercare.vo.ElderFamilyVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/elder-families")
public class ElderFamilyController {

    private final ElderFamilyService elderFamilyService;

    public ElderFamilyController(ElderFamilyService elderFamilyService) {
        this.elderFamilyService = elderFamilyService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('elder:family:list')")
    public Result<List<ElderFamilyVO>> list(@RequestParam(required = false) Long elderId,
                                            @RequestParam(required = false) Long familyUserId) {
        return Result.success(elderFamilyService.list(elderId, familyUserId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('elder:family:add')")
    public Result<Long> bind(@Valid @RequestBody ElderFamilyCreateDTO dto, HttpServletRequest request) {
        return Result.success(elderFamilyService.bind(dto, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('elder:family:delete')")
    public Result<Void> unbind(@PathVariable Long id, HttpServletRequest request) {
        elderFamilyService.unbind(id, request);
        return Result.success();
    }
}
