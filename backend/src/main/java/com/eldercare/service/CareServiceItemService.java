package com.eldercare.service;

import com.eldercare.common.PageResult;
import com.eldercare.dto.CareServiceItemCreateDTO;
import com.eldercare.dto.CareServiceItemQueryDTO;
import com.eldercare.dto.CareServiceItemUpdateDTO;
import com.eldercare.vo.CareServiceItemVO;
import jakarta.servlet.http.HttpServletRequest;

public interface CareServiceItemService {

    Long create(CareServiceItemCreateDTO dto, HttpServletRequest request);

    void update(Long id, CareServiceItemUpdateDTO dto, HttpServletRequest request);

    void delete(Long id, HttpServletRequest request);

    CareServiceItemVO getById(Long id);

    PageResult<CareServiceItemVO> page(CareServiceItemQueryDTO query);

    /** 家属端只读：启用中的服务项目目录 */
    java.util.List<CareServiceItemVO> listEnabledForFamily();
}
