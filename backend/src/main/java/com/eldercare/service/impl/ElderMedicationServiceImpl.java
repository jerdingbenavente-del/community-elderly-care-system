package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.CareOrderStatuses;
import com.eldercare.common.MedicationStatuses;
import com.eldercare.common.PageResult;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.ElderMedicationQueryDTO;
import com.eldercare.dto.ElderMedicationSaveDTO;
import com.eldercare.entity.CareServiceOrder;
import com.eldercare.entity.CareStaff;
import com.eldercare.entity.Elder;
import com.eldercare.entity.ElderMedication;
import com.eldercare.entity.ElderMedicationTime;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.CareServiceOrderMapper;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.mapper.ElderMedicationMapper;
import com.eldercare.mapper.ElderMedicationTimeMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.CareStaffIdentityService;
import com.eldercare.service.DataPermissionService;
import com.eldercare.service.ElderMedicationService;
import com.eldercare.service.MedicationReminderService;
import com.eldercare.service.OperationLogService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.ElderMedicationVO;
import com.eldercare.vo.MedicationReminderVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ElderMedicationServiceImpl implements ElderMedicationService {

    private final ElderMedicationMapper medicationMapper;
    private final ElderMedicationTimeMapper timeMapper;
    private final ElderMapper elderMapper;
    private final CareServiceOrderMapper careServiceOrderMapper;
    private final DataPermissionService dataPermissionService;
    private final CareStaffIdentityService careStaffIdentityService;
    private final MedicationReminderService medicationReminderService;
    private final OperationLogService operationLogService;

    public ElderMedicationServiceImpl(ElderMedicationMapper medicationMapper,
                                      ElderMedicationTimeMapper timeMapper,
                                      ElderMapper elderMapper,
                                      CareServiceOrderMapper careServiceOrderMapper,
                                      DataPermissionService dataPermissionService,
                                      CareStaffIdentityService careStaffIdentityService,
                                      MedicationReminderService medicationReminderService,
                                      OperationLogService operationLogService) {
        this.medicationMapper = medicationMapper;
        this.timeMapper = timeMapper;
        this.elderMapper = elderMapper;
        this.careServiceOrderMapper = careServiceOrderMapper;
        this.dataPermissionService = dataPermissionService;
        this.careStaffIdentityService = careStaffIdentityService;
        this.medicationReminderService = medicationReminderService;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public Long create(ElderMedicationSaveDTO dto, HttpServletRequest request) {
        requireAdmin();
        requireElder(dto.getElderId());
        List<LocalTime> times = parseTimes(dto.getDoseTimes());
        assertDateRange(dto);
        ElderMedication row = new ElderMedication();
        fill(row, dto);
        row.setStatus(MedicationStatuses.ACTIVE);
        row.setCreatedBy(SecurityUtils.requireLoginUser().getUserId());
        medicationMapper.insert(row);
        saveTimes(row.getId(), times);
        operationLogService.record("care", "MEDICATION_CREATE", "elder_medication",
                String.valueOf(row.getId()), "SUCCESS", request);
        return row.getId();
    }

    @Override
    @Transactional
    public void update(Long id, ElderMedicationSaveDTO dto, HttpServletRequest request) {
        requireAdmin();
        ElderMedication row = requireMedication(id);
        requireElder(dto.getElderId());
        List<LocalTime> times = parseTimes(dto.getDoseTimes());
        assertDateRange(dto);
        fill(row, dto);
        medicationMapper.updateById(row);
        timeMapper.delete(new LambdaQueryWrapper<ElderMedicationTime>()
                .eq(ElderMedicationTime::getMedicationId, id));
        saveTimes(id, times);
        operationLogService.record("care", "MEDICATION_UPDATE", "elder_medication",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    @Transactional
    public void changeStatus(Long id, String status, HttpServletRequest request) {
        requireAdmin();
        if (!MedicationStatuses.ACTIVE.equals(status) && !MedicationStatuses.INACTIVE.equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用药状态不合法");
        }
        ElderMedication row = requireMedication(id);
        row.setStatus(status);
        medicationMapper.updateById(row);
        operationLogService.record("care", "MEDICATION_STATUS", "elder_medication",
                id + "|" + status, "SUCCESS", request);
    }

    @Override
    @Transactional
    public void delete(Long id, HttpServletRequest request) {
        requireAdmin();
        requireMedication(id);
        medicationMapper.deleteById(id);
        timeMapper.delete(new LambdaQueryWrapper<ElderMedicationTime>()
                .eq(ElderMedicationTime::getMedicationId, id));
        operationLogService.record("care", "MEDICATION_DELETE", "elder_medication",
                String.valueOf(id), "SUCCESS", request);
    }

    @Override
    public ElderMedicationVO getById(Long id) {
        ElderMedication row = requireMedication(id);
        assertCanViewElder(row.getElderId());
        return toVo(row);
    }

    @Override
    public PageResult<ElderMedicationVO> pageAdmin(ElderMedicationQueryDTO query) {
        requireAdmin();
        return page(query, null);
    }

    @Override
    public PageResult<ElderMedicationVO> pageFamily(ElderMedicationQueryDTO query) {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (query.getElderId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请指定老人");
        }
        dataPermissionService.checkFamilyAccess(user.getUserId(), query.getElderId());
        return page(query, List.of(query.getElderId()));
    }

    @Override
    public PageResult<ElderMedicationVO> pageStaff(ElderMedicationQueryDTO query) {
        Set<Long> scope = staffElderIds();
        if (query.getElderId() != null && !scope.contains(query.getElderId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能查看自己服务范围内老人的用药");
        }
        List<Long> ids = query.getElderId() != null ? List.of(query.getElderId()) : new ArrayList<>(scope);
        if (ids.isEmpty()) {
            return PageResult.of(List.of(), 0, query.getPage(), query.getSize());
        }
        return page(query, ids);
    }

    @Override
    public List<MedicationReminderVO> todayRemindersForStaff() {
        return medicationReminderService.listForElders(staffElderIds(), LocalDateTime.now());
    }

    private PageResult<ElderMedicationVO> page(ElderMedicationQueryDTO query, List<Long> elderIds) {
        Page<ElderMedication> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<ElderMedication> wrapper = new LambdaQueryWrapper<ElderMedication>()
                .in(elderIds != null, ElderMedication::getElderId, elderIds == null || elderIds.isEmpty() ? List.of(-1L) : elderIds)
                .eq(query.getElderId() != null && elderIds == null, ElderMedication::getElderId, query.getElderId())
                .like(StringUtils.hasText(query.getMedicineName()), ElderMedication::getMedicineName, query.getMedicineName())
                .eq(StringUtils.hasText(query.getStatus()), ElderMedication::getStatus, query.getStatus())
                .le(query.getOnDate() != null, ElderMedication::getStartDate, query.getOnDate())
                .ge(query.getOnDate() != null, ElderMedication::getEndDate, query.getOnDate())
                .orderByDesc(ElderMedication::getId);
        if (elderIds != null && elderIds.isEmpty()) {
            return PageResult.of(List.of(), 0, query.getPage(), query.getSize());
        }
        Page<ElderMedication> result = medicationMapper.selectPage(page, wrapper);
        List<ElderMedicationVO> records = result.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private void assertCanViewElder(Long elderId) {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (user.hasRole("ADMIN")) {
            return;
        }
        if (user.hasRole("FAMILY") && !user.hasRole("CARE_STAFF") && !user.hasRole("ADMIN")) {
            dataPermissionService.checkFamilyAccess(user.getUserId(), elderId);
            return;
        }
        if (user.hasRole("CARE_STAFF")) {
            if (!staffElderIds().contains(elderId)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "只能查看自己服务范围内老人的用药");
            }
            return;
        }
        if (user.hasRole("FAMILY")) {
            dataPermissionService.checkFamilyAccess(user.getUserId(), elderId);
            return;
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "无权查看用药信息");
    }

    private Set<Long> staffElderIds() {
        CareStaff staff = careStaffIdentityService.requireCurrentCareStaff();
        return careServiceOrderMapper.selectList(new LambdaQueryWrapper<CareServiceOrder>()
                        .eq(CareServiceOrder::getCareStaffId, staff.getId())
                        .ne(CareServiceOrder::getStatus, CareOrderStatuses.CANCELLED))
                .stream()
                .map(CareServiceOrder::getElderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void requireAdmin() {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可维护用药计划");
        }
    }

    private Elder requireElder(Long elderId) {
        Elder elder = elderMapper.selectById(elderId);
        if (elder == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "老人不存在");
        }
        return elder;
    }

    private ElderMedication requireMedication(Long id) {
        ElderMedication row = medicationMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用药记录不存在");
        }
        return row;
    }

    private void assertDateRange(ElderMedicationSaveDTO dto) {
        if (dto.getStartDate() == null || dto.getEndDate() == null || dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "开始日期不能晚于结束日期");
        }
    }

    private void fill(ElderMedication row, ElderMedicationSaveDTO dto) {
        row.setElderId(dto.getElderId());
        row.setMedicineName(dto.getMedicineName().trim());
        row.setDosage(dto.getDosage().trim());
        row.setDosageUnit(dto.getDosageUnit().trim());
        row.setUsageMethod(dto.getUsageMethod().trim());
        row.setStartDate(dto.getStartDate());
        row.setEndDate(dto.getEndDate());
        row.setRemark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : null);
    }

    private List<LocalTime> parseTimes(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "至少填写一个服药时间");
        }
        Set<LocalTime> unique = new LinkedHashSet<>();
        for (String item : raw) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            String text = item.trim();
            if (text.length() == 5) {
                text = text + ":00";
            }
            try {
                unique.add(LocalTime.parse(text).withSecond(0).withNano(0));
            } catch (DateTimeParseException ex) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "服药时间格式应为 HH:mm");
            }
        }
        if (unique.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "至少填写一个服药时间");
        }
        return new ArrayList<>(unique);
    }

    private void saveTimes(Long medicationId, List<LocalTime> times) {
        for (LocalTime time : times) {
            ElderMedicationTime row = new ElderMedicationTime();
            row.setMedicationId(medicationId);
            row.setDoseTime(time);
            timeMapper.insert(row);
        }
    }

    private ElderMedicationVO toVo(ElderMedication row) {
        ElderMedicationVO vo = new ElderMedicationVO();
        vo.setId(row.getId());
        vo.setElderId(row.getElderId());
        vo.setMedicineName(row.getMedicineName());
        vo.setDosage(row.getDosage());
        vo.setDosageUnit(row.getDosageUnit());
        vo.setUsageMethod(row.getUsageMethod());
        vo.setStartDate(row.getStartDate());
        vo.setEndDate(row.getEndDate());
        vo.setStatus(row.getStatus());
        vo.setRemark(row.getRemark());
        vo.setCreatedBy(row.getCreatedBy());
        vo.setCreatedAt(row.getCreatedAt());
        vo.setUpdatedAt(row.getUpdatedAt());
        Elder elder = elderMapper.selectById(row.getElderId());
        if (elder != null) {
            vo.setElderName(elder.getName());
        }
        List<LocalTime> times = timeMapper.selectList(new LambdaQueryWrapper<ElderMedicationTime>()
                        .eq(ElderMedicationTime::getMedicationId, row.getId())
                        .orderByAsc(ElderMedicationTime::getDoseTime))
                .stream()
                .map(ElderMedicationTime::getDoseTime)
                .collect(Collectors.toList());
        vo.setDoseTimes(times);
        return vo;
    }
}
