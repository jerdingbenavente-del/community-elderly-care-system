package com.eldercare.service;

import com.eldercare.common.PageResult;
import com.eldercare.dto.ElderCreateDTO;
import com.eldercare.dto.ElderQueryDTO;
import com.eldercare.dto.ElderUpdateDTO;
import com.eldercare.vo.ElderVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface ElderService {

    PageResult<ElderVO> page(ElderQueryDTO query);

    ElderVO getById(Long id);

    Long create(ElderCreateDTO dto, HttpServletRequest request);

    void update(Long id, ElderUpdateDTO dto, HttpServletRequest request);

    /** 逻辑删除并停用，非物理删除 */
    void delete(Long id, HttpServletRequest request);

    List<ElderVO> listBoundEldersForCurrentFamily();

    ElderVO getBoundElderForCurrentFamily(Long elderId);
}
