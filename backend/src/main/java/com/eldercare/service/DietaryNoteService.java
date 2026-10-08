package com.eldercare.service;

import com.eldercare.dto.DietaryNoteSaveDTO;
import com.eldercare.vo.DietaryNoteVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface DietaryNoteService {

    DietaryNoteVO getForElder(Long elderId);

    DietaryNoteVO save(DietaryNoteSaveDTO dto, HttpServletRequest request);

    /** 管理员：只返回已有真实备注的老人，不造「无特殊要求」。 */
    List<DietaryNoteVO> listForAdmin();
}
