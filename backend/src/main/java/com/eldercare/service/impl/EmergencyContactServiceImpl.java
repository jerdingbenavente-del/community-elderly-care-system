package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.EmergencyContactCreateDTO;
import com.eldercare.dto.EmergencyContactUpdateDTO;
import com.eldercare.entity.Elder;
import com.eldercare.entity.EmergencyContact;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.mapper.EmergencyContactMapper;
import com.eldercare.service.DataPermissionService;
import com.eldercare.service.EmergencyContactService;
import com.eldercare.service.OperationLogService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.EmergencyContactVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmergencyContactServiceImpl implements EmergencyContactService {

    private final EmergencyContactMapper emergencyContactMapper;
    private final ElderMapper elderMapper;
    private final DataPermissionService dataPermissionService;
    private final OperationLogService operationLogService;

    public EmergencyContactServiceImpl(EmergencyContactMapper emergencyContactMapper,
                                       ElderMapper elderMapper,
                                       DataPermissionService dataPermissionService,
                                       OperationLogService operationLogService) {
        this.emergencyContactMapper = emergencyContactMapper;
        this.elderMapper = elderMapper;
        this.dataPermissionService = dataPermissionService;
        this.operationLogService = operationLogService;
    }

    @Override
    public List<EmergencyContactVO> listByElderId(Long elderId) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireElder(elderId);
        return queryByElder(elderId);
    }

    @Override
    public List<EmergencyContactVO> listByElderIdForFamily(Long elderId) {
        Long userId = SecurityUtils.requireUserId();
        dataPermissionService.checkFamilyAccess(userId, elderId);
        requireElder(elderId);
        return queryByElder(elderId);
    }

    @Override
    @Transactional
    public Long create(Long elderId, EmergencyContactCreateDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireElder(elderId);
        EmergencyContact contact = new EmergencyContact();
        contact.setElderId(elderId);
        contact.setName(dto.getName().trim());
        contact.setRelationship(dto.getRelationship());
        contact.setPhone(dto.getPhone().trim());
        contact.setPriority(dto.getPriority() == null ? 1 : dto.getPriority());
        contact.setRemark(dto.getRemark());
        emergencyContactMapper.insert(contact);
        operationLogService.record("elder", "EMERGENCY_CONTACT_CREATE", "emergency_contact",
                String.valueOf(contact.getId()), "SUCCESS", request);
        return contact.getId();
    }

    @Override
    @Transactional
    public void update(Long id, EmergencyContactUpdateDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        EmergencyContact contact = requireContact(id);
        contact.setName(dto.getName().trim());
        contact.setRelationship(dto.getRelationship());
        contact.setPhone(dto.getPhone().trim());
        contact.setPriority(dto.getPriority() == null ? contact.getPriority() : dto.getPriority());
        contact.setRemark(dto.getRemark());
        emergencyContactMapper.updateById(contact);
        operationLogService.record("elder", "EMERGENCY_CONTACT_UPDATE", "emergency_contact",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void delete(Long id, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireContact(id);
        emergencyContactMapper.deleteById(id);
        operationLogService.record("elder", "EMERGENCY_CONTACT_DELETE", "emergency_contact",
                String.valueOf(id), "SUCCESS", request);
    }

    private List<EmergencyContactVO> queryByElder(Long elderId) {
        return emergencyContactMapper.selectList(new LambdaQueryWrapper<EmergencyContact>()
                        .eq(EmergencyContact::getElderId, elderId)
                        .orderByAsc(EmergencyContact::getPriority)
                        .orderByAsc(EmergencyContact::getId))
                .stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    private Elder requireElder(Long elderId) {
        Elder elder = elderMapper.selectById(elderId);
        if (elder == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "老人不存在");
        }
        return elder;
    }

    private EmergencyContact requireContact(Long id) {
        EmergencyContact contact = emergencyContactMapper.selectById(id);
        if (contact == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "紧急联系人不存在");
        }
        return contact;
    }

    private EmergencyContactVO toVo(EmergencyContact contact) {
        EmergencyContactVO vo = new EmergencyContactVO();
        vo.setId(contact.getId());
        vo.setElderId(contact.getElderId());
        vo.setName(contact.getName());
        vo.setRelationship(contact.getRelationship());
        vo.setPhone(contact.getPhone());
        vo.setPriority(contact.getPriority());
        vo.setRemark(contact.getRemark());
        vo.setCreatedAt(contact.getCreatedAt());
        vo.setUpdatedAt(contact.getUpdatedAt());
        return vo;
    }
}
