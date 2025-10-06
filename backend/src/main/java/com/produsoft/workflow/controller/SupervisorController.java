package com.produsoft.workflow.controller;

import com.produsoft.workflow.domain.StageType;
import com.produsoft.workflow.dto.OrderMapper;
import com.produsoft.workflow.dto.OrderStageStatusResponse;
import com.produsoft.workflow.dto.SupervisorDecisionRequest;
import com.produsoft.workflow.dto.WipSummaryResponse;
import com.produsoft.workflow.service.OrderWorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/supervisor")
public class SupervisorController {

    private final OrderWorkflowService orderWorkflowService;
    private final OrderMapper mapper;

    public SupervisorController(OrderWorkflowService orderWorkflowService, OrderMapper mapper) {
        this.orderWorkflowService = orderWorkflowService;
        this.mapper = mapper;
    }

    @GetMapping("/wip")
    public WipSummaryResponse wipSummary() {
        return orderWorkflowService.buildWipSummary();
    }

    @PostMapping("/orders/{orderId}/stages/{stage}/approve-skip")
    public OrderStageStatusResponse approveSkip(@PathVariable Long orderId,
                                                @PathVariable String stage,
                                                @Valid @RequestBody SupervisorDecisionRequest request) {
        StageType stageType = StageType.fromString(stage);
        return mapper.toStageResponse(orderWorkflowService.approveSkip(orderId, stageType, request));
    }

    @PostMapping("/orders/{orderId}/stages/{stage}/request-rework")
    public OrderStageStatusResponse requestRework(@PathVariable Long orderId,
                                                  @PathVariable String stage,
                                                  @Valid @RequestBody SupervisorDecisionRequest request) {
        StageType stageType = StageType.fromString(stage);
        return mapper.toStageResponse(orderWorkflowService.requestRework(orderId, stageType, request));
    }
}
