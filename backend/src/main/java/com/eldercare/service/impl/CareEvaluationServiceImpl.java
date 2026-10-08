package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.CareOrderStatuses;
import com.eldercare.common.PageResult;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.CareEvaluationCreateDTO;
import com.eldercare.dto.CareEvaluationQueryDTO;
import com.eldercare.entity.CareEvaluation;
import com.eldercare.entity.CareServiceItem;
import com.eldercare.entity.CareServiceOrder;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.Elder;
import com.eldercare.entity.ElderFamily;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareEvaluationMapper;
import com.eldercare.mapper.CareServiceItemMapper;
import com.eldercare.mapper.CareServiceOrderMapper;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.mapper.ElderFamilyMapper;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.service.CareEvaluationService;
import com.eldercare.service.DataPermissionService;
import com.eldercare.service.OperationLogService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.CareEvaluationVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CareEvaluationServiceImpl implements CareEvaluationService {

    private final CareEvaluationMapper careEvaluationMapper;
    private final CareServiceOrderMapper careServiceOrderMapper;
    private final CareServiceItemMapper careServiceItemMapper;
    private final CareStaffMapper careStaffMapper;
    private final ElderMapper elderMapper;
    private final ElderFamilyMapper elderFamilyMapper;
    private final DataPermissionService dataPermissionService;
    private final OperationLogService operationLogService;

    public CareEvaluationServiceImpl(CareEvaluationMapper careEvaluationMapper,
                                     CareServiceOrderMapper careServiceOrderMapper,
                                     CareServiceItemMapper careServiceItemMapper,
                                     CareStaffMapper careStaffMapper,
                                     ElderMapper elderMapper,
                                     ElderFamilyMapper elderFamilyMapper,
                                     DataPermissionService dataPermissionService,
                                     OperationLogService operationLogService) {
        this.careEvaluationMapper = careEvaluationMapper;
        this.careServiceOrderMapper = careServiceOrderMapper;
        this.careServiceItemMapper = careServiceItemMapper;
        this.careStaffMapper = careStaffMapper;
        this.elderMapper = elderMapper;
        this.elderFamilyMapper = elderFamilyMapper;
        this.dataPermissionService = dataPermissionService;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public CareEvaluationVO createForFamily(Long orderId, CareEvaluationCreateDTO dto, HttpServletRequest request) {
        Long userId = SecurityUtils.requireUserId();
        CareServiceOrder order = requireOrder(orderId);
        if (!CareOrderStatuses.COMPLETED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "只有已完成的服务订单可以评价");
        }
        dataPermissionService.checkFamilyAccess(userId, order.getElderId());

        if (existsByServiceOrderId(orderId)) {
            throw new BusinessException(ResultCode.CONFLICT, "该订单已经评价");
        }

        CareEvaluation evaluation = new CareEvaluation();
        evaluation.setServiceOrderId(order.getId());
        evaluation.setElderId(order.getElderId());
        evaluation.setFamilyUserId(userId);
        evaluation.setScore(dto.getScore());
        if (StringUtils.hasText(dto.getContent())) {
            evaluation.setContent(dto.getContent().trim());
        }
        try {
            careEvaluationMapper.insert(evaluation);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ResultCode.CONFLICT, "该订单已经评价");
        }

        operationLogService.record("care", "CARE_EVALUATION_CREATE", "care_evaluation",
                String.valueOf(evaluation.getId()), "SUCCESS", request);
        return toVo(evaluation);
    }

    @Override
    public CareEvaluationVO getByOrderIdForFamily(Long orderId) {
        Long userId = SecurityUtils.requireUserId();
        CareServiceOrder order = requireOrder(orderId);
        dataPermissionService.checkFamilyAccess(userId, order.getElderId());
        CareEvaluation evaluation = careEvaluationMapper.selectOne(new LambdaQueryWrapper<CareEvaluation>()
                .eq(CareEvaluation::getServiceOrderId, orderId));
        if (evaluation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该订单尚未评价");
        }
        return toVo(evaluation);
    }

    @Override
    public List<CareEvaluationVO> listForFamily() {
        Long userId = SecurityUtils.requireUserId();
        List<Long> elderIds = listBoundElderIds(userId);
        if (elderIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<CareEvaluation> list = careEvaluationMapper.selectList(new LambdaQueryWrapper<CareEvaluation>()
                .in(CareEvaluation::getElderId, elderIds)
                .orderByDesc(CareEvaluation::getId));
        return toVoList(list);
    }

    @Override
    public PageResult<CareEvaluationVO> pageForAdmin(CareEvaluationQueryDTO query) {
        dataPermissionService.denyFamilyOnAdminApi();
        Page<CareEvaluation> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<CareEvaluation> wrapper = new LambdaQueryWrapper<>();
        if (query.getElderId() != null) {
            wrapper.eq(CareEvaluation::getElderId, query.getElderId());
        }
        if (StringUtils.hasText(query.getElderName())) {
            String keyword = query.getElderName().trim();
            List<Long> matchedElderIds = elderMapper.selectList(new LambdaQueryWrapper<Elder>()
                            .like(Elder::getName, keyword)
                            .select(Elder::getId))
                    .stream()
                    .map(Elder::getId)
                    .collect(Collectors.toList());
            if (matchedElderIds.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0, query.getPage(), query.getSize());
            }
            wrapper.in(CareEvaluation::getElderId, matchedElderIds);
        }
        if (query.getServiceOrderId() != null) {
            wrapper.eq(CareEvaluation::getServiceOrderId, query.getServiceOrderId());
        }
        if (query.getScore() != null) {
            wrapper.eq(CareEvaluation::getScore, query.getScore());
        }
        wrapper.orderByDesc(CareEvaluation::getId);
        Page<CareEvaluation> result = careEvaluationMapper.selectPage(page, wrapper);
        List<CareEvaluationVO> records = toVoList(result.getRecords());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public CareEvaluationVO getByIdForAdmin(Long id) {
        dataPermissionService.denyFamilyOnAdminApi();
        CareEvaluation evaluation = careEvaluationMapper.selectById(id);
        if (evaluation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }
        return toVo(evaluation);
    }

    private boolean existsByServiceOrderId(Long serviceOrderId) {
        Long count = careEvaluationMapper.selectCount(new LambdaQueryWrapper<CareEvaluation>()
                .eq(CareEvaluation::getServiceOrderId, serviceOrderId));
        return count != null && count > 0;
    }

    private CareServiceOrder requireOrder(Long orderId) {
        CareServiceOrder order = careServiceOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务订单不存在");
        }
        return order;
    }

    private List<Long> listBoundElderIds(Long familyUserId) {
        return elderFamilyMapper.selectList(new LambdaQueryWrapper<ElderFamily>()
                        .eq(ElderFamily::getFamilyUserId, familyUserId)
                        .eq(ElderFamily::getStatus, 1))
                .stream()
                .map(ElderFamily::getElderId)
                .collect(Collectors.toList());
    }

    private CareEvaluationVO toVo(CareEvaluation evaluation) {
        return toVoList(Collections.singletonList(evaluation)).get(0);
    }

    /**
     * 批量组装 VO：订单 / 服务项 / 护理员 / 老人各查一次，避免列表 N+1。
     */
    private List<CareEvaluationVO> toVoList(List<CareEvaluation> evaluations) {
        if (evaluations == null || evaluations.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> orderIds = new HashSet<>();
        Set<Long> elderIds = new HashSet<>();
        for (CareEvaluation e : evaluations) {
            if (e.getServiceOrderId() != null) {
                orderIds.add(e.getServiceOrderId());
            }
            if (e.getElderId() != null) {
                elderIds.add(e.getElderId());
            }
        }

        Map<Long, CareServiceOrder> orderMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            careServiceOrderMapper.selectBatchIds(orderIds).forEach(o -> orderMap.put(o.getId(), o));
        }

        Set<Long> itemIds = new HashSet<>();
        Set<Long> staffIds = new HashSet<>();
        for (CareServiceOrder order : orderMap.values()) {
            if (order.getServiceItemId() != null) {
                itemIds.add(order.getServiceItemId());
            }
            if (order.getCareStaffId() != null) {
                staffIds.add(order.getCareStaffId());
            }
        }

        Map<Long, CareServiceItem> itemMap = new HashMap<>();
        if (!itemIds.isEmpty()) {
            careServiceItemMapper.selectBatchIds(itemIds).forEach(i -> itemMap.put(i.getId(), i));
        }

        Map<Long, CareStaff> staffMap = new HashMap<>();
        if (!staffIds.isEmpty()) {
            careStaffMapper.selectBatchIds(staffIds).forEach(s -> staffMap.put(s.getId(), s));
        }

        Map<Long, Elder> elderMap = new HashMap<>();
        if (!elderIds.isEmpty()) {
            elderMapper.selectBatchIds(elderIds).forEach(elder -> elderMap.put(elder.getId(), elder));
        }

        return evaluations.stream().map(evaluation -> {
            CareEvaluationVO vo = new CareEvaluationVO();
            vo.setId(evaluation.getId());
            vo.setServiceOrderId(evaluation.getServiceOrderId());
            vo.setElderId(evaluation.getElderId());
            vo.setScore(evaluation.getScore());
            vo.setContent(evaluation.getContent());
            vo.setCreatedAt(evaluation.getCreatedAt());

            CareServiceOrder order = orderMap.get(evaluation.getServiceOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
                vo.setServiceItemId(order.getServiceItemId());
                vo.setCareStaffId(order.getCareStaffId());
                CareServiceItem item = itemMap.get(order.getServiceItemId());
                if (item != null) {
                    vo.setServiceName(item.getServiceName());
                }
                if (order.getCareStaffId() != null) {
                    CareStaff staff = staffMap.get(order.getCareStaffId());
                    if (staff != null) {
                        vo.setCareStaffName(staff.getName());
                    }
                }
            }

            Elder elder = elderMap.get(evaluation.getElderId());
            if (elder != null) {
                vo.setElderName(elder.getName());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
