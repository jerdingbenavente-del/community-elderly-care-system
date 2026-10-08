package com.eldercare.controller;

import com.eldercare.common.Result;
import com.eldercare.dto.WeeklyMenuSaveDTO;
import com.eldercare.service.WeeklyMenuService;
import com.eldercare.vo.ElderMenuViewVO;
import com.eldercare.vo.WeeklyMenuVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/weekly-menus")
public class WeeklyMenuController {

    private final WeeklyMenuService weeklyMenuService;

    public WeeklyMenuController(WeeklyMenuService weeklyMenuService) {
        this.weeklyMenuService = weeklyMenuService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('care:menu:list')")
    public Result<List<WeeklyMenuVO>> list() {
        return Result.success(weeklyMenuService.listRecent());
    }

    @GetMapping("/by-week")
    @PreAuthorize("hasAuthority('care:menu:view')")
    public Result<WeeklyMenuVO> byWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return Result.success(weeklyMenuService.getByWeek(weekStart));
    }

    @GetMapping("/elder-view")
    @PreAuthorize("hasAuthority('care:menu:view')")
    public Result<ElderMenuViewVO> elderView(
            @RequestParam Long elderId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return Result.success(weeklyMenuService.elderView(elderId, weekStart));
    }

    @GetMapping("/staff-today")
    @PreAuthorize("hasAuthority('care:menu:view')")
    public Result<List<ElderMenuViewVO>> staffToday() {
        return Result.success(weeklyMenuService.staffToday());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('care:menu:add')")
    public Result<WeeklyMenuVO> create(@Valid @RequestBody WeeklyMenuSaveDTO dto, HttpServletRequest request) {
        return Result.success(weeklyMenuService.create(dto, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('care:menu:update')")
    public Result<WeeklyMenuVO> update(@PathVariable Long id,
                                       @Valid @RequestBody WeeklyMenuSaveDTO dto,
                                       HttpServletRequest request) {
        return Result.success(weeklyMenuService.update(id, dto, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('care:menu:delete')")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        weeklyMenuService.delete(id, request);
        return Result.success();
    }
}
