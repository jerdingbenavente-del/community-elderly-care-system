package com.eldercare.service;

import com.eldercare.dto.EmergencyContactCreateDTO;
import com.eldercare.dto.EmergencyContactUpdateDTO;
import com.eldercare.vo.EmergencyContactVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface EmergencyContactService {

    List<EmergencyContactVO> listByElderId(Long elderId);

    Long create(Long elderId, EmergencyContactCreateDTO dto, HttpServletRequest request);

    void update(Long id, EmergencyContactUpdateDTO dto, HttpServletRequest request);

    void delete(Long id, HttpServletRequest request);

    List<EmergencyContactVO> listByElderIdForFamily(Long elderId);
}
