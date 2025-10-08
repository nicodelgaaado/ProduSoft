package com.produsoft.workflow.controller;

import com.produsoft.workflow.domain.StageState;
import com.produsoft.workflow.domain.StageType;
import com.produsoft.workflow.dto.ClaimStageRequest;
import com.produsoft.workflow.dto.CompleteStageRequest;
import com.produsoft.workflow.dto.FlagStageExceptionRequest;
import com.produsoft.workflow.dto.OrderMapper;
import com.produsoft.workflow.dto.OrderStageStatusResponse;
import com.produsoft.workflow.dto.WorkQueueItemResponse;
import com.produsoft.workflow.dto.UpdateChecklistItemRequest;
import com.produsoft.workflow.service.OrderWorkflowService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operator")
public class OperatorController {

    private final OrderWorkflowService orderWorkflowService;
    private final OrderMapper mapper;

    public OperatorController(OrderWorkflowService orderWorkflowService, OrderMapper mapper) {
        this.orderWorkflowService = orderWorkflowService;
        this.mapper = mapper;
    }

    @GetMapping("/queue")
    public List<WorkQueueItemResponse> getQueue(@RequestParam("stage") String stage,
                                                @RequestParam(value = "states", required = false) List<String> states) {
        StageType stageType = StageType.fromString(stage);
        List<StageState> parsedStates = parseStates(states);
        return orderWorkflowService.fetchQueue(stageType, parsedStates).stream()
            .map(mapper::toQueueItem)
            .collect(Collectors.toList());
    }

    @PostMapping("/orders/{orderId}/stages/{stage}/claim")
    public OrderStageStatusResponse claim(@PathVariable Long orderId,
                                          @PathVariable String stage,
                                          @Valid @RequestBody ClaimStageRequest request) {
        StageType stageType = StageType.fromString(stage);
        return mapper.toStageResponse(orderWorkflowService.claimStage(orderId, stageType, request.assignee()));
    }

    @PostMapping("/orders/{orderId}/stages/{stage}/complete")
    public OrderStageStatusResponse complete(@PathVariable Long orderId,
                                             @PathVariable String stage,
                                             @Valid @RequestBody CompleteStageRequest request) {
        StageType stageType = StageType.fromString(stage);
        return mapper.toStageResponse(orderWorkflowService.completeStage(orderId, stageType, request));
    }

    @PostMapping("/orders/{orderId}/stages/{stage}/flag-exception")
    public OrderStageStatusResponse flagException(@PathVariable Long orderId,
                                                  @PathVariable String stage,
                                                  @Valid @RequestBody FlagStageExceptionRequest request) {
        StageType stageType = StageType.fromString(stage);
        return mapper.toStageResponse(orderWorkflowService.flagException(orderId, stageType, request));
    }

    @PatchMapping("/orders/{orderId}/stages/{stage}/checklist")
    public OrderStageStatusResponse updateChecklist(@PathVariable Long orderId,
                                                    @PathVariable String stage,
                                                    @Valid @RequestBody UpdateChecklistItemRequest request) {
        StageType stageType = StageType.fromString(stage);
        return mapper.toStageResponse(orderWorkflowService.updateChecklistItem(orderId, stageType, request));
    }

    private List<StageState> parseStates(List<String> states) {
        if (states == null || states.isEmpty()) {
            return List.of();
        }
        return states.stream()
            .map(value -> StageState.valueOf(value.toUpperCase(Locale.ROOT)))
            .collect(Collectors.toList());
    }
}
