package com.eldercare.controller;

import com.eldercare.common.Result;
import com.eldercare.dto.DietaryNoteSaveDTO;
import com.eldercare.service.DietaryNoteService;
import com.eldercare.vo.DietaryNoteVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dietary-notes")
public class DietaryNoteController {

    private final DietaryNoteService dietaryNoteService;

    public DietaryNoteController(DietaryNoteService dietaryNoteService) {
        this.dietaryNoteService = dietaryNoteService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('care:dietary:admin:list')")
    public Result<List<DietaryNoteVO>> adminList() {
        return Result.success(dietaryNoteService.listForAdmin());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('care:dietary:view')")
    public Result<DietaryNoteVO> get(@RequestParam Long elderId) {
        return Result.success(dietaryNoteService.getForElder(elderId));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('care:dietary:update')")
    public Result<DietaryNoteVO> save(@Valid @RequestBody DietaryNoteSaveDTO dto, HttpServletRequest request) {
        return Result.success(dietaryNoteService.save(dto, request));
    }
}
