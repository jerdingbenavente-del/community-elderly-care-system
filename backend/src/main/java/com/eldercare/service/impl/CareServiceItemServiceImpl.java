package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.CareOrderStatuses;
import com.eldercare.common.PageResult;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.CareServiceItemCreateDTO;
import com.eldercare.dto.CareServiceItemQueryDTO;
import com.eldercare.dto.CareServiceItemUpdateDTO;
import com.eldercare.entity.CareServiceItem;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareServiceItemMapper;
import com.eldercare.service.CareServiceItemService;
import com.eldercare.service.DataPermissionService;
import com.eldercare.service.OperationLogService;
import com.eldercare.vo.CareServiceItemVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CareServiceItemServiceImpl implements CareServiceItemService {

    private final CareServiceItemMapper careServiceItemMapper;
    private final DataPermissionService dataPermissionService;
    private final OperationLogService operationLogService;

    public CareServiceItemServiceImpl(CareServiceItemMapper careServiceItemMapper,
                                      DataPermissionService dataPermissionService,
                                      OperationLogService operationLogService) {
        this.careServiceItemMapper = careServiceItemMapper;
        this.dataPermissionService = dataPermissionService;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public Long create(CareServiceItemCreateDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        String code = dto.getServiceCode().trim();
        Long exists = careServiceItemMapper.selectCount(new LambdaQueryWrapper<CareServiceItem>()
                .eq(CareServiceItem::getServiceCode, code));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "服务编码已存在");
        }
        CareServiceItem item = new CareServiceItem();
        item.setServiceCode(code);
        item.setServiceName(dto.getServiceName().trim());
        item.setServiceType(dto.getServiceType());
        item.setDescription(dto.getDescription());
        item.setDurationMinutes(dto.getDurationMinutes());
        item.setPrice(dto.getPrice());
        item.setStatus(CareOrderStatuses.ITEM_ENABLED);
        careServiceItemMapper.insert(item);
        operationLogService.record("care", "SERVICE_ITEM_CREATE", "care_service_item",
                String.valueOf(item.getId()), "SUCCESS", request);
        return item.getId();
    }

    @Override
    @Transactional
    public void update(Long id, CareServiceItemUpdateDTO dto, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        CareServiceItem item = requireItem(id);
        item.setServiceName(dto.getServiceName().trim());
        item.setServiceType(dto.getServiceType());
        item.setDescription(dto.getDescription());
        item.setDurationMinutes(dto.getDurationMinutes());
        item.setPrice(dto.getPrice());
        item.setStatus(dto.getStatus());
        careServiceItemMapper.updateById(item);
        operationLogService.record("care", "SERVICE_ITEM_UPDATE", "care_service_item",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void delete(Long id, HttpServletRequest request) {
        dataPermissionService.denyFamilyOnAdminApi();
        requireItem(id);
        careServiceItemMapper.deleteById(id);
        operationLogService.record("care", "SERVICE_ITEM_DELETE", "care_service_item",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    public CareServiceItemVO getById(Long id) {
        return toVo(requireItem(id));
    }

    @Override
    public PageResult<CareServiceItemVO> page(CareServiceItemQueryDTO query) {
        Page<CareServiceItem> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<CareServiceItem> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getServiceName())) {
            wrapper.like(CareServiceItem::getServiceName, query.getServiceName().trim());
        }
        if (StringUtils.hasText(query.getServiceCode())) {
            wrapper.eq(CareServiceItem::getServiceCode, query.getServiceCode().trim());
        }
        if (StringUtils.hasText(query.getServiceType())) {
            wrapper.eq(CareServiceItem::getServiceType, query.getServiceType().trim());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(CareServiceItem::getStatus, query.getStatus().trim());
        }
        wrapper.orderByDesc(CareServiceItem::getId);
        Page<CareServiceItem> result = careServiceItemMapper.selectPage(page, wrapper);
        List<CareServiceItemVO> records = result.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private CareServiceItem requireItem(Long id) {
        CareServiceItem item = careServiceItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务项目不存在");
        }
        return item;
    }


    @Override
    public List<CareServiceItemVO> listEnabledForFamily() {
        List<CareServiceItem> list = careServiceItemMapper.selectList(new LambdaQueryWrapper<CareServiceItem>()
                .eq(CareServiceItem::getStatus, CareOrderStatuses.ITEM_ENABLED)
                .orderByAsc(CareServiceItem::getId));
        return list.stream().map(this::toVo).collect(Collectors.toList());
    }

    private CareServiceItemVO toVo(CareServiceItem item) {
        CareServiceItemVO vo = new CareServiceItemVO();
        vo.setId(item.getId());
        vo.setServiceCode(item.getServiceCode());
        vo.setServiceName(item.getServiceName());
        vo.setServiceType(item.getServiceType());
        vo.setDescription(item.getDescription());
        vo.setDurationMinutes(item.getDurationMinutes());
        vo.setPrice(item.getPrice());
        vo.setStatus(item.getStatus());
        vo.setCreatedAt(item.getCreatedAt());
        vo.setUpdatedAt(item.getUpdatedAt());
        return vo;
    }
}
