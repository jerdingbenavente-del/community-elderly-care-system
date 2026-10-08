package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.ElderFamilyCreateDTO;
import com.eldercare.entity.Elder;
import com.eldercare.entity.ElderFamily;
import com.eldercare.entity.SysUser;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.ElderFamilyMapper;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.service.DataPermissionService;
import com.eldercare.service.ElderFamilyService;
import com.eldercare.service.OperationLogService;
import com.eldercare.vo.ElderFamilyVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ElderFamilyServiceImpl implements ElderFamilyService {

    private final ElderFamilyMapper elderFamilyMapper;
    private final ElderMapper elderMapper;
    private final SysUserMapper sysUserMapper;
    private final DataPermissionService dataPermissionService;
    private final OperationLogService operationLogService;

    public ElderFamilyServiceImpl(ElderFamilyMapper elderFamilyMapper,
                                  ElderMapper elderMapper,
                                  SysUserMapper sysUserMapper,
                                  DataPermissionService dataPermissionService,
                                  OperationLogService operationLogService) {
        this.elderFamilyMapper = elderFamilyMapper;
        this.elderMapper = elderMapper;
        this.sysUserMapper = sysUserMapper;
        this.dataPermissionService = dataPermissionService;
        this.operationLogService = operationLogService;
    }

    @Override
    public List<ElderFamilyVO> list(Long elderId, Long familyUserId) {
        dataPermissionService.denyFamilyOnAdminApi();
        LambdaQueryWrapper<ElderFamily> wrapper = new LambdaQueryWrapper<>();
        if (elderId != null) {
            wrapper.eq(ElderFamily::getElderId, elderId);
        }
        if (familyUserId != null) {
            wrapper.eq(ElderFamily::getFamilyUserId, familyUserId);
        }
        wrapper.orderByDesc(ElderFamily::getId);
        return elderFamilyMapper.selectList(wrapper).stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long bind(ElderFamilyCreateDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        Elder elder = elderMapper.selectById(dto.getElderId());
        if (elder == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "老人不存在");
        }
        SysUser user = sysUserMapper.selectById(dto.getFamilyUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "家属用户不存在");
        }
        List<String> roleCodes = sysUserMapper.selectRoleCodesByUserId(dto.getFamilyUserId());
        if (roleCodes == null || !roleCodes.contains("FAMILY")) {
            throw new BusinessException(ResultCode.CONFLICT, "当前系统用户不是家属角色，无法绑定老人");
        }

        ElderFamily existing = elderFamilyMapper.selectAnyByElderAndUser(dto.getElderId(), dto.getFamilyUserId());
        if (existing != null && existing.getDeleted() != null && existing.getDeleted() == 0
                && existing.getStatus() != null && existing.getStatus() == 1) {
            throw new BusinessException(ResultCode.CONFLICT, "该家属已绑定此老人，请勿重复绑定");
        }

        Integer isPrimary = dto.getIsPrimary() == null ? 0 : dto.getIsPrimary();
        if (existing != null) {
            existing.setRelationship(dto.getRelationship());
            existing.setIsPrimary(isPrimary);
            elderFamilyMapper.restoreById(existing);
            operationLogService.record("elder", "FAMILY_BIND", "elder_family",
                    String.valueOf(existing.getId()), "SUCCESS", request);
            return existing.getId();
        }

        ElderFamily binding = new ElderFamily();
        binding.setElderId(dto.getElderId());
        binding.setFamilyUserId(dto.getFamilyUserId());
        binding.setRelationship(dto.getRelationship());
        binding.setIsPrimary(isPrimary);
        binding.setStatus(1);
        elderFamilyMapper.insert(binding);
        operationLogService.record("elder", "FAMILY_BIND", "elder_family",
                String.valueOf(binding.getId()), "SUCCESS", request);
        return binding.getId();
    }

    @Override
    @Transactional
    public void unbind(Long id, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        ElderFamily binding = elderFamilyMapper.selectById(id);
        if (binding == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "绑定关系不存在");
        }
        binding.setStatus(0);
        elderFamilyMapper.updateById(binding);
        elderFamilyMapper.deleteById(id);
        operationLogService.record("elder", "FAMILY_UNBIND", "elder_family",
                String.valueOf(id), "SUCCESS", request);
    }

    private ElderFamilyVO toVo(ElderFamily binding) {
        ElderFamilyVO vo = new ElderFamilyVO();
        vo.setId(binding.getId());
        vo.setElderId(binding.getElderId());
        vo.setFamilyUserId(binding.getFamilyUserId());
        vo.setRelationship(binding.getRelationship());
        vo.setIsPrimary(binding.getIsPrimary());
        vo.setStatus(binding.getStatus());
        vo.setCreatedAt(binding.getCreatedAt());
        Elder elder = elderMapper.selectById(binding.getElderId());
        if (elder != null) {
            vo.setElderName(elder.getName());
        }
        SysUser user = sysUserMapper.selectById(binding.getFamilyUserId());
        if (user != null) {
            vo.setFamilyUsername(user.getUsername());
            vo.setFamilyRealName(user.getRealName());
        }
        return vo;
    }
}
