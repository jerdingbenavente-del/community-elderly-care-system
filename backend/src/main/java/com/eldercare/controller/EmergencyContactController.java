package com.eldercare.controller;

import com.eldercare.common.Result;
import com.eldercare.dto.EmergencyContactCreateDTO;
import com.eldercare.dto.EmergencyContactUpdateDTO;
import com.eldercare.service.EmergencyContactService;
import com.eldercare.vo.EmergencyContactVO;
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
@RequestMapping("/api")
public class EmergencyContactController {

    private final EmergencyContactService emergencyContactService;

    public EmergencyContactController(EmergencyContactService emergencyContactService) {
        this.emergencyContactService = emergencyContactService;
    }

    @GetMapping("/elders/{elderId}/emergency-contacts")
    @PreAuthorize("hasAuthority('elder:emergency-contact:list')")
    public Result<List<EmergencyContactVO>> list(@PathVariable Long elderId) {
        return Result.success(emergencyContactService.listByElderId(elderId));
    }

    @PostMapping("/elders/{elderId}/emergency-contacts")
    @PreAuthorize("hasAuthority('elder:emergency-contact:add')")
    public Result<Long> create(@PathVariable Long elderId,
                               @Valid @RequestBody EmergencyContactCreateDTO dto,
                               HttpServletRequest request) {
        return Result.success(emergencyContactService.create(elderId, dto, request));
    }

    @PutMapping("/emergency-contacts/{id}")
    @PreAuthorize("hasAuthority('elder:emergency-contact:update')")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody EmergencyContactUpdateDTO dto,
                               HttpServletRequest request) {
        emergencyContactService.update(id, dto, request);
        return Result.success();
    }

    @DeleteMapping("/emergency-contacts/{id}")
    @PreAuthorize("hasAuthority('elder:emergency-contact:delete')")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        emergencyContactService.delete(id, request);
        return Result.success();
    }
}
