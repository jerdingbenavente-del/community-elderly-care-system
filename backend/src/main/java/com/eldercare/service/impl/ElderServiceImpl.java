package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.PageResult;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.ElderCreateDTO;
import com.eldercare.dto.ElderQueryDTO;
import com.eldercare.dto.ElderUpdateDTO;
import com.eldercare.entity.Elder;
import com.eldercare.entity.ElderFamily;
import com.eldercare.entity.EmergencyContact;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.ElderFamilyMapper;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.mapper.EmergencyContactMapper;
import com.eldercare.service.DataPermissionService;
import com.eldercare.service.ElderService;
import com.eldercare.service.OperationLogService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.utils.SensitiveUtils;
import com.eldercare.vo.ElderVO;
import com.eldercare.vo.EmergencyContactVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ElderServiceImpl implements ElderService {

    private final ElderMapper elderMapper;
    private final ElderFamilyMapper elderFamilyMapper;
    private final EmergencyContactMapper emergencyContactMapper;
    private final DataPermissionService dataPermissionService;
    private final OperationLogService operationLogService;

    public ElderServiceImpl(ElderMapper elderMapper,
                            ElderFamilyMapper elderFamilyMapper,
                            EmergencyContactMapper emergencyContactMapper,
                            DataPermissionService dataPermissionService,
                            OperationLogService operationLogService) {
        this.elderMapper = elderMapper;
        this.elderFamilyMapper = elderFamilyMapper;
        this.emergencyContactMapper = emergencyContactMapper;
        this.dataPermissionService = dataPermissionService;
        this.operationLogService = operationLogService;
    }

    @Override
    public PageResult<ElderVO> page(ElderQueryDTO query) {
        dataPermissionService.denyFamilyOnAdminApi();
        Page<Elder> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Elder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(Elder::getName, query.getName().trim());
        }
        if (StringUtils.hasText(query.getPhone())) {
            wrapper.eq(Elder::getPhone, query.getPhone().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Elder::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Elder::getId);
        Page<Elder> result = elderMapper.selectPage(page, wrapper);
        List<ElderVO> records = result.getRecords().stream()
                .map(e -> toVo(e, false, false))
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public ElderVO getById(Long id) {
        dataPermissionService.denyFamilyOnAdminApi();
        Elder elder = requireElder(id);
        return toVo(elder, true, true);
    }

    @Override
    @Transactional
    public Long create(ElderCreateDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        Elder elder = new Elder();
        fillFromCreate(elder, dto);
        if (elder.getStatus() == null) {
            elder.setStatus(1);
        }
        elderMapper.insert(elder);
        operationLogService.record("elder", "ELDER_CREATE", "elder",
                String.valueOf(elder.getId()), "SUCCESS", request);
        return elder.getId();
    }

    @Override
    @Transactional
    public void update(Long id, ElderUpdateDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        Elder elder = requireElder(id);
        fillFromUpdate(elder, dto);
        elderMapper.updateById(elder);
        operationLogService.record("elder", "ELDER_UPDATE", "elder",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void delete(Long id, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        Elder elder = requireElder(id);
        elder.setStatus(0);
        elderMapper.updateById(elder);
        elderMapper.deleteById(id);
        operationLogService.record("elder", "ELDER_DISABLE", "elder",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    public List<ElderVO> listBoundEldersForCurrentFamily() {
        Long userId = SecurityUtils.requireUserId();
        List<ElderFamily> bindings = elderFamilyMapper.selectList(new LambdaQueryWrapper<ElderFamily>()
                .eq(ElderFamily::getFamilyUserId, userId)
                .eq(ElderFamily::getStatus, 1));
        return bindings.stream()
                .map(b -> elderMapper.selectById(b.getElderId()))
                .filter(e -> e != null)
                .map(e -> toVo(e, false, false))
                .collect(Collectors.toList());
    }

    @Override
    public ElderVO getBoundElderForCurrentFamily(Long elderId) {
        Long userId = SecurityUtils.requireUserId();
        dataPermissionService.checkFamilyAccess(userId, elderId);
        Elder elder = requireElder(elderId);
        return toVo(elder, true, true);
    }

    private Elder requireElder(Long id) {
        Elder elder = elderMapper.selectById(id);
        if (elder == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "老人不存在");
        }
        return elder;
    }

    private void fillFromCreate(Elder elder, ElderCreateDTO dto) {
        elder.setName(dto.getName().trim());
        elder.setGender(dto.getGender());
        elder.setBirthDate(dto.getBirthDate());
        elder.setPhone(blankToNull(dto.getPhone()));
        elder.setAddress(blankToNull(dto.getAddress()));
        elder.setIdCard(blankToNull(dto.getIdCard()));
        elder.setCareLevel(blankToNull(dto.getCareLevel()));
        elder.setStatus(dto.getStatus());
        elder.setRegisteredAt(dto.getRegisteredAt());
        elder.setMedicalHistory(blankToNull(dto.getMedicalHistory()));
        elder.setAllergyHistory(blankToNull(dto.getAllergyHistory()));
        elder.setSpecialCareRequirement(blankToNull(dto.getSpecialCareRequirement()));
        elder.setRemark(blankToNull(dto.getRemark()));
    }

    private void fillFromUpdate(Elder elder, ElderUpdateDTO dto) {
        elder.setName(dto.getName().trim());
        elder.setGender(dto.getGender());
        elder.setBirthDate(dto.getBirthDate());
        elder.setPhone(blankToNull(dto.getPhone()));
        elder.setAddress(blankToNull(dto.getAddress()));
        elder.setIdCard(blankToNull(dto.getIdCard()));
        elder.setCareLevel(blankToNull(dto.getCareLevel()));
        elder.setStatus(dto.getStatus());
        elder.setRegisteredAt(dto.getRegisteredAt());
        elder.setMedicalHistory(blankToNull(dto.getMedicalHistory()));
        elder.setAllergyHistory(blankToNull(dto.getAllergyHistory()));
        elder.setSpecialCareRequirement(blankToNull(dto.getSpecialCareRequirement()));
        elder.setRemark(blankToNull(dto.getRemark()));
    }

    private ElderVO toVo(Elder elder, boolean detail, boolean withContacts) {
        ElderVO vo = new ElderVO();
        vo.setId(elder.getId());
        vo.setName(elder.getName());
        vo.setGender(elder.getGender());
        vo.setBirthDate(elder.getBirthDate());
        vo.setPhone(elder.getPhone());
        vo.setAddress(elder.getAddress());
        vo.setIdCard(SensitiveUtils.maskIdCard(elder.getIdCard()));
        vo.setCareLevel(elder.getCareLevel());
        vo.setStatus(elder.getStatus());
        vo.setRegisteredAt(elder.getRegisteredAt());
        if (detail) {
            vo.setMedicalHistory(elder.getMedicalHistory());
            vo.setAllergyHistory(elder.getAllergyHistory());
            vo.setSpecialCareRequirement(elder.getSpecialCareRequirement());
            vo.setRemark(elder.getRemark());
        }
        vo.setCreatedAt(elder.getCreatedAt());
        vo.setUpdatedAt(elder.getUpdatedAt());
        if (withContacts) {
            List<EmergencyContact> contacts = emergencyContactMapper.selectList(
                    new LambdaQueryWrapper<EmergencyContact>()
                            .eq(EmergencyContact::getElderId, elder.getId())
                            .orderByAsc(EmergencyContact::getPriority)
                            .orderByAsc(EmergencyContact::getId));
            vo.setEmergencyContacts(contacts.stream().map(this::toContactVo).collect(Collectors.toList()));
        }
        return vo;
    }

    private EmergencyContactVO toContactVo(EmergencyContact contact) {
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

    private String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
