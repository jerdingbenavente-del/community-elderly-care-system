package com.eldercare.controller;

import com.eldercare.common.MedicationStatuses;
import com.eldercare.common.PageResult;
import com.eldercare.common.Result;
import com.eldercare.dto.ElderMedicationQueryDTO;
import com.eldercare.dto.ElderMedicationSaveDTO;
import com.eldercare.service.ElderMedicationService;
import com.eldercare.vo.ElderMedicationVO;
import com.eldercare.vo.MedicationReminderVO;
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

import java.util.List;

@RestController
@RequestMapping("/api/elder-medications")
public class ElderMedicationController {

    private final ElderMedicationService elderMedicationService;

    public ElderMedicationController(ElderMedicationService elderMedicationService) {
        this.elderMedicationService = elderMedicationService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('care:medication:admin:list')")
    public Result<PageResult<ElderMedicationVO>> adminPage(@Valid ElderMedicationQueryDTO query) {
        return Result.success(elderMedicationService.pageAdmin(query));
    }

    @GetMapping("/family")
    @PreAuthorize("hasAuthority('care:medication:list')")
    public Result<PageResult<ElderMedicationVO>> familyPage(@Valid ElderMedicationQueryDTO query) {
        return Result.success(elderMedicationService.pageFamily(query));
    }

    @GetMapping("/staff")
    @PreAuthorize("hasAuthority('care:medication:list')")
    public Result<PageResult<ElderMedicationVO>> staffPage(@Valid ElderMedicationQueryDTO query) {
        return Result.success(elderMedicationService.pageStaff(query));
    }

    @GetMapping("/today-reminders")
    @PreAuthorize("hasAuthority('care:medication:remind')")
    public Result<List<MedicationReminderVO>> todayReminders() {
        return Result.success(elderMedicationService.todayRemindersForStaff());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('care:medication:view')")
    public Result<ElderMedicationVO> detail(@PathVariable Long id) {
        return Result.success(elderMedicationService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('care:medication:add')")
    public Result<Long> create(@Valid @RequestBody ElderMedicationSaveDTO dto, HttpServletRequest request) {
        return Result.success(elderMedicationService.create(dto, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('care:medication:update')")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody ElderMedicationSaveDTO dto,
                               HttpServletRequest request) {
        elderMedicationService.update(id, dto, request);
        return Result.success();
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('care:medication:update')")
    public Result<Void> enable(@PathVariable Long id, HttpServletRequest request) {
        elderMedicationService.changeStatus(id, MedicationStatuses.ACTIVE, request);
        return Result.success();
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('care:medication:update')")
    public Result<Void> disable(@PathVariable Long id, HttpServletRequest request) {
        elderMedicationService.changeStatus(id, MedicationStatuses.INACTIVE, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('care:medication:delete')")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        elderMedicationService.delete(id, request);
        return Result.success();
    }
}
