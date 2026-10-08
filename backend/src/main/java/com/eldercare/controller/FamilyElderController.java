package com.eldercare.controller;

import com.eldercare.common.Result;
import com.eldercare.dto.HealthRecordQueryDTO;
import com.eldercare.service.ElderService;
import com.eldercare.service.EmergencyContactService;
import com.eldercare.service.HealthRecordService;
import com.eldercare.service.HealthWarningService;
import com.eldercare.vo.ElderVO;
import com.eldercare.vo.EmergencyContactVO;
import com.eldercare.vo.HealthRecordVO;
import com.eldercare.vo.HealthWarningVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 家属端接口：强制走 JWT 当前用户 + elder_family 绑定校验。
 */
@RestController
@RequestMapping("/api/family")
public class FamilyElderController {

    private final ElderService elderService;
    private final EmergencyContactService emergencyContactService;
    private final HealthRecordService healthRecordService;
    private final HealthWarningService healthWarningService;

    public FamilyElderController(ElderService elderService,
                                 EmergencyContactService emergencyContactService,
                                 HealthRecordService healthRecordService,
                                 HealthWarningService healthWarningService) {
        this.elderService = elderService;
        this.emergencyContactService = emergencyContactService;
        this.healthRecordService = healthRecordService;
        this.healthWarningService = healthWarningService;
    }

    @GetMapping("/elders")
    @PreAuthorize("hasAuthority('family:elder:list')")
    public Result<List<ElderVO>> listMyElders() {
        return Result.success(elderService.listBoundEldersForCurrentFamily());
    }

    @GetMapping("/elders/{elderId}")
    @PreAuthorize("hasAuthority('family:elder:view')")
    public Result<ElderVO> getMyElder(@PathVariable Long elderId) {
        return Result.success(elderService.getBoundElderForCurrentFamily(elderId));
    }

    @GetMapping("/elders/{elderId}/emergency-contacts")
    @PreAuthorize("hasAuthority('family:elder:view')")
    public Result<List<EmergencyContactVO>> listContacts(@PathVariable Long elderId) {
        return Result.success(emergencyContactService.listByElderIdForFamily(elderId));
    }

    @GetMapping("/elders/{elderId}/health-records")
    @PreAuthorize("hasAuthority('family:health:list')")
    public Result<List<HealthRecordVO>> listHealthRecords(@PathVariable Long elderId,
                                                          @Valid HealthRecordQueryDTO query) {
        return Result.success(healthRecordService.listForFamily(elderId, query));
    }

    @GetMapping("/health-records/{id}")
    @PreAuthorize("hasAuthority('family:health:view')")
    public Result<HealthRecordVO> getHealthRecord(@PathVariable Long id) {
        return Result.success(healthRecordService.getByIdForFamily(id));
    }

    @GetMapping("/elders/{elderId}/health-warnings")
    @PreAuthorize("hasAuthority('family:health:list')")
    public Result<List<HealthWarningVO>> listHealthWarnings(@PathVariable Long elderId) {
        return Result.success(healthWarningService.listByElderForFamily(elderId));
    }

    @GetMapping("/health-warnings/{id}")
    @PreAuthorize("hasAuthority('family:health:view')")
    public Result<HealthWarningVO> getHealthWarning(@PathVariable Long id) {
        return Result.success(healthWarningService.getByIdForFamily(id));
    }
}
