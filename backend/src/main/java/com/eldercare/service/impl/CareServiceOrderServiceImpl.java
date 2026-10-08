package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.CareOrderStatuses;
import com.eldercare.common.CarePaymentStatuses;
import com.eldercare.common.CareStaffStatuses;
import com.eldercare.common.PageResult;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.CancelServiceOrderDTO;
import com.eldercare.dto.ConfirmServiceOrderDTO;
import com.eldercare.dto.ServiceOrderCreateDTO;
import com.eldercare.dto.ServiceOrderQueryDTO;
import com.eldercare.entity.CareServiceItem;
import com.eldercare.entity.CareServiceOrder;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.Elder;
import com.eldercare.entity.ElderFamily;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareServiceItemMapper;
import com.eldercare.mapper.CareServiceOrderMapper;
import com.eldercare.mapper.CareStaffMapper;
import com.eldercare.mapper.ElderFamilyMapper;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.CareServiceOrderService;
import com.eldercare.service.CareStaffIdentityService;
import com.eldercare.service.CareStaffScheduleService;
import com.eldercare.service.DataPermissionService;
import com.eldercare.service.OperationLogService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.ServiceOrderVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class CareServiceOrderServiceImpl implements CareServiceOrderService {

    private static final DateTimeFormatter ORDER_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CareServiceOrderMapper careServiceOrderMapper;
    private final CareServiceItemMapper careServiceItemMapper;
    private final CareStaffMapper careStaffMapper;
    private final ElderMapper elderMapper;
    private final ElderFamilyMapper elderFamilyMapper;
    private final CareStaffScheduleService careStaffScheduleService;
    private final CareStaffIdentityService careStaffIdentityService;
    private final DataPermissionService dataPermissionService;
    private final OperationLogService operationLogService;

    public CareServiceOrderServiceImpl(CareServiceOrderMapper careServiceOrderMapper,
                                       CareServiceItemMapper careServiceItemMapper,
                                       CareStaffMapper careStaffMapper,
                                       ElderMapper elderMapper,
                                       ElderFamilyMapper elderFamilyMapper,
                                       CareStaffScheduleService careStaffScheduleService,
                                       CareStaffIdentityService careStaffIdentityService,
                                       DataPermissionService dataPermissionService,
                                       OperationLogService operationLogService) {
        this.careServiceOrderMapper = careServiceOrderMapper;
        this.careServiceItemMapper = careServiceItemMapper;
        this.careStaffMapper = careStaffMapper;
        this.elderMapper = elderMapper;
        this.elderFamilyMapper = elderFamilyMapper;
        this.careStaffScheduleService = careStaffScheduleService;
        this.careStaffIdentityService = careStaffIdentityService;
        this.dataPermissionService = dataPermissionService;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public Long create(ServiceOrderCreateDTO dto, HttpServletRequest request) {
        LoginUser user = SecurityUtils.requireLoginUser();
        Long elderId = dto.getElderId();

        // 家属业务：只要拥有 FAMILY 且非 ADMIN，就必须校验绑定（含 FAMILY+CARE_STAFF 多角色）
        if (user.hasRole("FAMILY") && !user.hasRole("ADMIN")) {
            dataPermissionService.checkFamilyAccess(user.getUserId(), elderId);
        }

        Elder elder = elderMapper.selectById(elderId);
        if (elder == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "老人不存在");
        }
        if (elder.getStatus() == null || elder.getStatus() != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "老人已停用，不能预约服务");
        }

        CareServiceItem item = careServiceItemMapper.selectById(dto.getServiceItemId());
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务项目不存在");
        }
        if (!CareOrderStatuses.ITEM_ENABLED.equals(item.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "服务项目已停用");
        }

        LocalDateTime start = dto.getScheduledStartTime();
        LocalDateTime end = dto.getScheduledEndTime();
        if (!start.isBefore(end)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "开始时间必须早于结束时间");
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes != item.getDurationMinutes().longValue()) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "预约时长须与服务项目时长一致（" + item.getDurationMinutes() + "分钟）");
        }

        // P4：创建订单一律不指定护理员，仅 confirm 时由 ADMIN 分配
        CareServiceOrder order = new CareServiceOrder();
        order.setOrderNo(generateOrderNo());
        order.setElderId(elderId);
        order.setServiceItemId(item.getId());
        order.setScheduledStartTime(start);
        order.setScheduledEndTime(end);
        order.setCareStaffId(null);
        order.setStatus(CareOrderStatuses.PENDING);
        // G2：金额快照，禁止信任前端传价
        order.setAmount(item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO);
        order.setPaymentStatus(CarePaymentStatuses.UNPAID);
        order.setPaidAt(null);
        order.setRemark(dto.getRemark());
        order.setCreatedBy(user.getUserId());
        careServiceOrderMapper.insert(order);

        operationLogService.record("care", "SERVICE_ORDER_CREATE", "care_service_order",
                String.valueOf(order.getId()), "SUCCESS", request);
        return order.getId();
    }

    @Override
    public PageResult<ServiceOrderVO> page(ServiceOrderQueryDTO query) {
        LoginUser user = SecurityUtils.requireLoginUser();
        Page<CareServiceOrder> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<CareServiceOrder> wrapper = buildQueryWrapper(query, user);
        wrapper.orderByDesc(CareServiceOrder::getId);
        Page<CareServiceOrder> result = careServiceOrderMapper.selectPage(page, wrapper);
        List<ServiceOrderVO> records = result.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public ServiceOrderVO getById(Long id) {
        LoginUser user = SecurityUtils.requireLoginUser();
        CareServiceOrder order = requireOrder(id);
        assertCanView(user, order);
        return toVo(order);
    }

    @Override
    @Transactional
    public void confirm(Long id, ConfirmServiceOrderDTO dto, HttpServletRequest request) {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可确认订单");
        }
        if (dto == null || dto.getCareStaffId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "护理员ID不能为空");
        }
        CareServiceOrder order = requireOrder(id);
        assertTransition(order.getStatus(), CareOrderStatuses.PENDING, CareOrderStatuses.CONFIRMED);

        Long careStaffId = dto.getCareStaffId();
        CareStaff staff = careStaffMapper.selectById(careStaffId);
        if (staff == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "护理人员不存在");
        }
        if (!CareStaffStatuses.isEnabled(staff.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "护理人员已停用");
        }

        LocalDateTime start = order.getScheduledStartTime();
        LocalDateTime end = order.getScheduledEndTime();
        careStaffScheduleService.assertScheduleCovers(careStaffId, start, end);
        assertNoStaffConflict(careStaffId, start, end, order.getId());

        order.setCareStaffId(careStaffId);
        order.setStatus(CareOrderStatuses.CONFIRMED);
        careServiceOrderMapper.updateById(order);
        operationLogService.record("care", "SERVICE_ORDER_CONFIRM_ASSIGN", "care_service_order",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void start(Long id, HttpServletRequest request) {
        LoginUser user = SecurityUtils.requireLoginUser();
        CareServiceOrder order = requireOrder(id);
        assertCanExecute(user, order);
        assertAssignedStaffEnabled(order);
        assertTransition(order.getStatus(), CareOrderStatuses.CONFIRMED, CareOrderStatuses.IN_SERVICE);
        order.setStatus(CareOrderStatuses.IN_SERVICE);
        careServiceOrderMapper.updateById(order);
        operationLogService.record("care", "SERVICE_ORDER_START", "care_service_order",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void complete(Long id, HttpServletRequest request) {
        LoginUser user = SecurityUtils.requireLoginUser();
        CareServiceOrder order = requireOrder(id);
        assertCanExecute(user, order);
        assertTransition(order.getStatus(), CareOrderStatuses.IN_SERVICE, CareOrderStatuses.COMPLETED);
        order.setStatus(CareOrderStatuses.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        careServiceOrderMapper.updateById(order);
        operationLogService.record("care", "SERVICE_ORDER_COMPLETE", "care_service_order",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void cancel(Long id, CancelServiceOrderDTO dto, HttpServletRequest request) {
        LoginUser user = SecurityUtils.requireLoginUser();
        CareServiceOrder order = requireOrder(id);
        assertCanCancel(user, order);
        String status = order.getStatus();
        if (!CareOrderStatuses.PENDING.equals(status) && !CareOrderStatuses.CONFIRMED.equals(status)) {
            throw new BusinessException(ResultCode.CONFLICT, "当前状态不允许取消");
        }
        order.setStatus(CareOrderStatuses.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        if (dto != null && StringUtils.hasText(dto.getCancelReason())) {
            order.setCancelReason(dto.getCancelReason().trim());
        }
        careServiceOrderMapper.updateById(order);
        operationLogService.record("care", "SERVICE_ORDER_CANCEL", "care_service_order",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void pay(Long id, HttpServletRequest request) {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("FAMILY") || user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅家属可进行模拟支付");
        }
        CareServiceOrder order = requireOrder(id);
        dataPermissionService.checkFamilyAccess(user.getUserId(), order.getElderId());

        if (CarePaymentStatuses.PAID.equals(order.getPaymentStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "订单已支付，请勿重复支付");
        }
        if (!CarePaymentStatuses.UNPAID.equals(order.getPaymentStatus())
                && StringUtils.hasText(order.getPaymentStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "当前支付状态不允许支付");
        }
        if (!CareOrderStatuses.PENDING.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "仅待确认且未支付的订单可支付");
        }

        order.setPaymentStatus(CarePaymentStatuses.PAID);
        order.setPaidAt(LocalDateTime.now());
        careServiceOrderMapper.updateById(order);
        operationLogService.record("care", "SERVICE_ORDER_PAY", "care_service_order",
                String.valueOf(id), "SUCCESS", request);
    }

    private LambdaQueryWrapper<CareServiceOrder> buildQueryWrapper(ServiceOrderQueryDTO query, LoginUser user) {
        LambdaQueryWrapper<CareServiceOrder> wrapper = new LambdaQueryWrapper<>();
        if (user.hasRole("ADMIN")) {
            if (query.getElderId() != null) {
                wrapper.eq(CareServiceOrder::getElderId, query.getElderId());
            }
            if (query.getCareStaffId() != null) {
                wrapper.eq(CareServiceOrder::getCareStaffId, query.getCareStaffId());
            }
        } else {
            boolean asFamily = user.hasRole("FAMILY");
            boolean asStaff = user.hasRole("CARE_STAFF");
            if (!asFamily && !asStaff) {
                wrapper.eq(CareServiceOrder::getId, -1L);
                return wrapper;
            }
            List<Long> elderIds = asFamily ? listBoundElderIds(user.getUserId()) : List.of();
            CareStaff staff = asStaff ? careStaffIdentityService.getByUserId(user.getUserId()) : null;

            if (query.getElderId() != null) {
                // 多角色也禁止用 elderId 越权查看未绑定老人
                if (asFamily && !elderIds.contains(query.getElderId())) {
                    throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该老人数据");
                }
                if (asFamily) {
                    wrapper.eq(CareServiceOrder::getElderId, query.getElderId());
                } else {
                    if (staff == null) {
                        wrapper.eq(CareServiceOrder::getId, -1L);
                        return wrapper;
                    }
                    wrapper.eq(CareServiceOrder::getCareStaffId, staff.getId())
                            .eq(CareServiceOrder::getElderId, query.getElderId());
                }
            } else if (asFamily && asStaff) {
                if (staff == null && elderIds.isEmpty()) {
                    wrapper.eq(CareServiceOrder::getId, -1L);
                    return wrapper;
                }
                if (staff == null) {
                    wrapper.in(CareServiceOrder::getElderId, elderIds);
                } else if (elderIds.isEmpty()) {
                    wrapper.eq(CareServiceOrder::getCareStaffId, staff.getId());
                } else {
                    Long staffId = staff.getId();
                    wrapper.and(w -> w.in(CareServiceOrder::getElderId, elderIds)
                            .or()
                            .eq(CareServiceOrder::getCareStaffId, staffId));
                }
            } else if (asFamily) {
                if (elderIds.isEmpty()) {
                    wrapper.eq(CareServiceOrder::getId, -1L);
                    return wrapper;
                }
                wrapper.in(CareServiceOrder::getElderId, elderIds);
            } else {
                if (staff == null) {
                    wrapper.eq(CareServiceOrder::getId, -1L);
                    return wrapper;
                }
                if (query.getCareStaffId() != null && !query.getCareStaffId().equals(staff.getId())) {
                    throw new BusinessException(ResultCode.FORBIDDEN, "只能查看分配给自己的订单");
                }
                wrapper.eq(CareServiceOrder::getCareStaffId, staff.getId());
            }
        }
        if (query.getServiceItemId() != null) {
            wrapper.eq(CareServiceOrder::getServiceItemId, query.getServiceItemId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(CareServiceOrder::getStatus, query.getStatus().trim());
        }
        if (query.getScheduledStartFrom() != null) {
            wrapper.ge(CareServiceOrder::getScheduledStartTime, query.getScheduledStartFrom());
        }
        if (query.getScheduledStartTo() != null) {
            wrapper.le(CareServiceOrder::getScheduledStartTime, query.getScheduledStartTo());
        }
        return wrapper;
    }

    private void assertCanView(LoginUser user, CareServiceOrder order) {
        if (user.hasRole("ADMIN")) {
            return;
        }
        boolean allowed = false;
        if (user.hasRole("FAMILY")
                && dataPermissionService.isFamilyBound(user.getUserId(), order.getElderId())) {
            allowed = true;
        }
        if (!allowed && user.hasRole("CARE_STAFF")) {
            CareStaff staff = careStaffIdentityService.getByUserId(user.getUserId());
            if (staff != null && order.getCareStaffId() != null
                    && staff.getId().equals(order.getCareStaffId())) {
                allowed = true;
            }
        }
        if (!allowed) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该订单");
        }
    }

    private void assertCanExecute(LoginUser user, CareServiceOrder order) {
        if (user.hasRole("ADMIN")) {
            return;
        }
        if (user.hasRole("CARE_STAFF")) {
            CareStaff staff = careStaffIdentityService.requireEnabledCurrentCareStaff();
            if (order.getCareStaffId() == null || !staff.getId().equals(order.getCareStaffId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "护理人员只能操作分配给自己的订单");
            }
            return;
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "无权执行该操作");
    }

    private void assertAssignedStaffEnabled(CareServiceOrder order) {
        if (order.getCareStaffId() == null) {
            throw new BusinessException(ResultCode.CONFLICT, "订单未分配护理员");
        }
        CareStaff staff = careStaffMapper.selectById(order.getCareStaffId());
        if (staff == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "护理人员不存在");
        }
        if (!CareStaffStatuses.isEnabled(staff.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "护理员已停用，不能开始服务");
        }
    }

    private void assertCanCancel(LoginUser user, CareServiceOrder order) {
        if (user.hasRole("ADMIN")) {
            return;
        }
        // 只要带 FAMILY 角色走取消，就必须校验绑定（含多角色）
        if (user.hasRole("FAMILY")) {
            dataPermissionService.checkFamilyAccess(user.getUserId(), order.getElderId());
            return;
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "无权取消该订单");
    }

    private void assertTransition(String current, String expectedFrom, String target) {
        if (!expectedFrom.equals(current)) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "订单状态不允许从 " + current + " 转换为 " + target);
        }
    }

    private void assertNoStaffConflict(Long careStaffId, LocalDateTime start, LocalDateTime end, Long excludeOrderId) {
        List<CareServiceOrder> existing = careServiceOrderMapper.selectList(new LambdaQueryWrapper<CareServiceOrder>()
                .eq(CareServiceOrder::getCareStaffId, careStaffId)
                .in(CareServiceOrder::getStatus, Arrays.asList(CareOrderStatuses.CONFIRMED, CareOrderStatuses.IN_SERVICE)));
        for (CareServiceOrder o : existing) {
            if (excludeOrderId != null && excludeOrderId.equals(o.getId())) {
                continue;
            }
            boolean overlap = start.isBefore(o.getScheduledEndTime()) && end.isAfter(o.getScheduledStartTime());
            if (overlap) {
                throw new BusinessException(ResultCode.CONFLICT, "护理人员在该时段已有冲突订单");
            }
        }
    }

    private List<Long> listBoundElderIds(Long familyUserId) {
        return elderFamilyMapper.selectList(new LambdaQueryWrapper<ElderFamily>()
                        .eq(ElderFamily::getFamilyUserId, familyUserId)
                        .eq(ElderFamily::getStatus, 1))
                .stream()
                .map(ElderFamily::getElderId)
                .collect(Collectors.toList());
    }

    private CareServiceOrder requireOrder(Long id) {
        CareServiceOrder order = careServiceOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "服务订单不存在");
        }
        return order;
    }

    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(ORDER_TS) + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private ServiceOrderVO toVo(CareServiceOrder order) {
        ServiceOrderVO vo = new ServiceOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setElderId(order.getElderId());
        vo.setServiceItemId(order.getServiceItemId());
        vo.setScheduledStartTime(order.getScheduledStartTime());
        vo.setScheduledEndTime(order.getScheduledEndTime());
        vo.setCareStaffId(order.getCareStaffId());
        vo.setStatus(order.getStatus());
        vo.setAmount(order.getAmount());
        vo.setPaymentStatus(order.getPaymentStatus());
        vo.setPaidAt(order.getPaidAt());
        vo.setRemark(order.getRemark());
        vo.setCancelReason(order.getCancelReason());
        vo.setCreatedBy(order.getCreatedBy());
        vo.setCompletedAt(order.getCompletedAt());
        vo.setCancelledAt(order.getCancelledAt());
        vo.setCreatedAt(order.getCreatedAt());
        Elder elder = elderMapper.selectById(order.getElderId());
        if (elder != null) {
            vo.setElderName(elder.getName());
        }
        // 服务项目可能已逻辑删除，历史订单仍需展示名称快照字段
        CareServiceItem item = careServiceItemMapper.selectByIdIncludeDeleted(order.getServiceItemId());
        if (item != null) {
            vo.setServiceName(item.getServiceName());
            vo.setServiceType(item.getServiceType());
            vo.setDurationMinutes(item.getDurationMinutes());
        }
        if (order.getCareStaffId() != null) {
            CareStaff staff = careStaffMapper.selectById(order.getCareStaffId());
            if (staff != null) {
                vo.setCareStaffName(staff.getName());
            }
        }
        return vo;
    }
}
