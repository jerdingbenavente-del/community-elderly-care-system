package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.MenuStatuses;
import com.eldercare.common.ResultCode;
import com.eldercare.dto.DietaryNoteSaveDTO;
import com.eldercare.entity.Elder;
import com.eldercare.entity.ElderDietaryNote;
import com.eldercare.exception.BusinessException;
import com.eldercare.mapper.ElderDietaryNoteMapper;
import com.eldercare.mapper.ElderMapper;
import com.eldercare.security.LoginUser;
import com.eldercare.service.DataPermissionService;
import com.eldercare.service.DietaryNoteService;
import com.eldercare.service.OperationLogService;
import com.eldercare.utils.SecurityUtils;
import com.eldercare.vo.DietaryNoteVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class DietaryNoteServiceImpl implements DietaryNoteService {

    private final ElderDietaryNoteMapper dietaryNoteMapper;
    private final ElderMapper elderMapper;
    private final DataPermissionService dataPermissionService;
    private final OperationLogService operationLogService;

    public DietaryNoteServiceImpl(ElderDietaryNoteMapper dietaryNoteMapper,
                                  ElderMapper elderMapper,
                                  DataPermissionService dataPermissionService,
                                  OperationLogService operationLogService) {
        this.dietaryNoteMapper = dietaryNoteMapper;
        this.elderMapper = elderMapper;
        this.dataPermissionService = dataPermissionService;
        this.operationLogService = operationLogService;
    }

    @Override
    public DietaryNoteVO getForElder(Long elderId) {
        requireElder(elderId);
        assertCanView(elderId);
        ElderDietaryNote note = dietaryNoteMapper.selectOne(new LambdaQueryWrapper<ElderDietaryNote>()
                .eq(ElderDietaryNote::getElderId, elderId)
                .eq(ElderDietaryNote::getStatus, MenuStatuses.DIETARY_ACTIVE));
        if (note == null || !StringUtils.hasText(note.getNote())) {
            return empty(elderId);
        }
        return toVo(note);
    }

    @Override
    @Transactional
    public DietaryNoteVO save(DietaryNoteSaveDTO dto, HttpServletRequest request) {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("FAMILY") && !user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅家属或管理员可维护饮食备注");
        }
        requireElder(dto.getElderId());
        if (user.hasRole("FAMILY")) {
            dataPermissionService.checkFamilyAccess(user.getUserId(), dto.getElderId());
        } else if (!user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权维护饮食备注");
        }

        String text = dto.getNote() == null ? "" : dto.getNote().trim();
        ElderDietaryNote existing = dietaryNoteMapper.selectAnyByElderId(dto.getElderId());

        // 清空备注：不保留「无特殊要求」空记录
        if (!StringUtils.hasText(text)) {
            if (existing != null && (existing.getDeleted() == null || existing.getDeleted() == 0)) {
                dietaryNoteMapper.deleteById(existing.getId());
                operationLogService.record("care", "DIETARY_NOTE_CLEAR", "elder_dietary_note",
                        String.valueOf(existing.getId()), "SUCCESS", request);
            }
            return empty(dto.getElderId());
        }

        if (existing == null) {
            ElderDietaryNote row = new ElderDietaryNote();
            row.setElderId(dto.getElderId());
            row.setNote(text);
            row.setStatus(MenuStatuses.DIETARY_ACTIVE);
            row.setUpdatedBy(user.getUserId());
            dietaryNoteMapper.insert(row);
            operationLogService.record("care", "DIETARY_NOTE_SAVE", "elder_dietary_note",
                    String.valueOf(row.getId()), "SUCCESS", request);
            return toVo(row);
        }

        if (existing.getDeleted() != null && existing.getDeleted() == 1) {
            existing.setNote(text);
            existing.setStatus(MenuStatuses.DIETARY_ACTIVE);
            existing.setUpdatedBy(user.getUserId());
            dietaryNoteMapper.restore(existing);
        } else {
            existing.setNote(text);
            existing.setStatus(MenuStatuses.DIETARY_ACTIVE);
            existing.setUpdatedBy(user.getUserId());
            dietaryNoteMapper.updateById(existing);
        }
        operationLogService.record("care", "DIETARY_NOTE_SAVE", "elder_dietary_note",
                String.valueOf(existing.getId()), "SUCCESS", request);
        return toVo(dietaryNoteMapper.selectById(existing.getId()));
    }

    @Override
    public List<DietaryNoteVO> listForAdmin() {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (!user.hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可查看全部饮食备注");
        }
        List<ElderDietaryNote> rows = dietaryNoteMapper.selectList(new LambdaQueryWrapper<ElderDietaryNote>()
                .eq(ElderDietaryNote::getStatus, MenuStatuses.DIETARY_ACTIVE)
                .orderByDesc(ElderDietaryNote::getUpdatedAt));
        List<DietaryNoteVO> result = new ArrayList<>();
        for (ElderDietaryNote row : rows) {
            if (!StringUtils.hasText(row.getNote())) {
                continue;
            }
            result.add(toVo(row));
        }
        return result;
    }

    private void assertCanView(Long elderId) {
        LoginUser user = SecurityUtils.requireLoginUser();
        if (user.hasRole("ADMIN")) {
            return;
        }
        if (user.hasRole("FAMILY")) {
            dataPermissionService.checkFamilyAccess(user.getUserId(), elderId);
            return;
        }
        if (user.hasRole("CARE_STAFF")) {
            return;
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "无权查看饮食备注");
    }

    private Elder requireElder(Long elderId) {
        Elder elder = elderMapper.selectById(elderId);
        if (elder == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "老人不存在");
        }
        return elder;
    }

    private DietaryNoteVO empty(Long elderId) {
        DietaryNoteVO vo = new DietaryNoteVO();
        vo.setElderId(elderId);
        Elder elder = elderMapper.selectById(elderId);
        if (elder != null) {
            vo.setElderName(elder.getName());
        }
        return vo;
    }

    private DietaryNoteVO toVo(ElderDietaryNote row) {
        DietaryNoteVO vo = new DietaryNoteVO();
        vo.setId(row.getId());
        vo.setElderId(row.getElderId());
        vo.setNote(row.getNote());
        vo.setStatus(row.getStatus());
        vo.setUpdatedAt(row.getUpdatedAt());
        Elder elder = elderMapper.selectById(row.getElderId());
        if (elder != null) {
            vo.setElderName(elder.getName());
        }
        return vo;
    }
}
