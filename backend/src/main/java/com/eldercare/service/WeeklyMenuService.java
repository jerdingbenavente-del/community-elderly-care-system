package com.eldercare.service;

import com.eldercare.dto.WeeklyMenuSaveDTO;
import com.eldercare.vo.ElderMenuViewVO;
import com.eldercare.vo.WeeklyMenuVO;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyMenuService {

    WeeklyMenuVO create(WeeklyMenuSaveDTO dto, HttpServletRequest request);

    WeeklyMenuVO update(Long id, WeeklyMenuSaveDTO dto, HttpServletRequest request);

    void delete(Long id, HttpServletRequest request);

    WeeklyMenuVO getByWeek(LocalDate dateInWeek);

    List<WeeklyMenuVO> listRecent();

    ElderMenuViewVO elderView(Long elderId, LocalDate dateInWeek);

    List<ElderMenuViewVO> staffToday();
}
