package com.eldercare.service;

import com.eldercare.common.PageResult;
import com.eldercare.dto.CancelServiceOrderDTO;
import com.eldercare.dto.ConfirmServiceOrderDTO;
import com.eldercare.dto.ServiceOrderCreateDTO;
import com.eldercare.dto.ServiceOrderQueryDTO;
import com.eldercare.vo.ServiceOrderVO;
import jakarta.servlet.http.HttpServletRequest;

public interface CareServiceOrderService {

    Long create(ServiceOrderCreateDTO dto, HttpServletRequest request);

    PageResult<ServiceOrderVO> page(ServiceOrderQueryDTO query);

    ServiceOrderVO getById(Long id);

    void confirm(Long id, ConfirmServiceOrderDTO dto, HttpServletRequest request);

    void start(Long id, HttpServletRequest request);

    void complete(Long id, HttpServletRequest request);

    void cancel(Long id, CancelServiceOrderDTO dto, HttpServletRequest request);

    /** FAMILY 模拟支付：仅 PENDING+UNPAID，校验老人绑定 */
    void pay(Long id, HttpServletRequest request);
}
