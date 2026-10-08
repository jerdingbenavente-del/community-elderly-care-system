package com.eldercare.controller;

import com.eldercare.common.PageResult;
import com.eldercare.common.Result;
import com.eldercare.dto.CancelServiceOrderDTO;
import com.eldercare.dto.ConfirmServiceOrderDTO;
import com.eldercare.dto.ServiceOrderCreateDTO;
import com.eldercare.dto.ServiceOrderQueryDTO;
import com.eldercare.service.CareServiceOrderService;
import com.eldercare.vo.ServiceOrderVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-orders")
public class ServiceOrderController {

    private final CareServiceOrderService careServiceOrderService;

    public ServiceOrderController(CareServiceOrderService careServiceOrderService) {
        this.careServiceOrderService = careServiceOrderService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('care:order:list','family:order:list')")
    public Result<PageResult<ServiceOrderVO>> page(@Valid ServiceOrderQueryDTO query) {
        return Result.success(careServiceOrderService.page(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('care:order:view','family:order:view')")
    public Result<ServiceOrderVO> detail(@PathVariable Long id) {
        return Result.success(careServiceOrderService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('care:order:add','family:order:add')")
    public Result<Long> create(@Valid @RequestBody ServiceOrderCreateDTO dto, HttpServletRequest request) {
        return Result.success(careServiceOrderService.create(dto, request));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('care:order:confirm')")
    public Result<Void> confirm(@PathVariable Long id,
                                @Valid @RequestBody ConfirmServiceOrderDTO dto,
                                HttpServletRequest request) {
        careServiceOrderService.confirm(id, dto, request);
        return Result.success();
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('care:order:start')")
    public Result<Void> start(@PathVariable Long id, HttpServletRequest request) {
        careServiceOrderService.start(id, request);
        return Result.success();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('care:order:complete')")
    public Result<Void> complete(@PathVariable Long id, HttpServletRequest request) {
        careServiceOrderService.complete(id, request);
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('care:order:cancel','family:order:cancel')")
    public Result<Void> cancel(@PathVariable Long id,
                               @RequestBody(required = false) @Valid CancelServiceOrderDTO dto,
                               HttpServletRequest request) {
        careServiceOrderService.cancel(id, dto == null ? new CancelServiceOrderDTO() : dto, request);
        return Result.success();
    }

    /** G2 家属模拟支付：PENDING + UNPAID → paymentStatus=PAID，status 不变 */
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('family:order:pay')")
    public Result<Void> pay(@PathVariable Long id, HttpServletRequest request) {
        careServiceOrderService.pay(id, request);
        return Result.success();
    }
}
