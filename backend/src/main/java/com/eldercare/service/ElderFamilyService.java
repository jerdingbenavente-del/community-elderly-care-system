package com.eldercare.service;

import com.eldercare.dto.ElderFamilyCreateDTO;
import com.eldercare.vo.ElderFamilyVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface ElderFamilyService {

    List<ElderFamilyVO> list(Long elderId, Long familyUserId);

    Long bind(ElderFamilyCreateDTO dto, HttpServletRequest request);

    void unbind(Long id, HttpServletRequest request);
}
