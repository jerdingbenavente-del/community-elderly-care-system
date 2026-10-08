package com.eldercare.service;

import com.eldercare.common.PageResult;
import com.eldercare.dto.ElderMedicationQueryDTO;
import com.eldercare.dto.ElderMedicationSaveDTO;
import com.eldercare.vo.ElderMedicationVO;
import com.eldercare.vo.MedicationReminderVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface ElderMedicationService {

    Long create(ElderMedicationSaveDTO dto, HttpServletRequest request);

    void update(Long id, ElderMedicationSaveDTO dto, HttpServletRequest request);

    void changeStatus(Long id, String status, HttpServletRequest request);

    void delete(Long id, HttpServletRequest request);

    ElderMedicationVO getById(Long id);

    PageResult<ElderMedicationVO> pageAdmin(ElderMedicationQueryDTO query);

    PageResult<ElderMedicationVO> pageFamily(ElderMedicationQueryDTO query);

    PageResult<ElderMedicationVO> pageStaff(ElderMedicationQueryDTO query);

    List<MedicationReminderVO> todayRemindersForStaff();
}
