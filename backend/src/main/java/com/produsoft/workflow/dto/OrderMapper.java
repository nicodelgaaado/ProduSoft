package com.produsoft.workflow.dto;

import com.produsoft.workflow.domain.Order;
import com.produsoft.workflow.domain.OrderStageStatus;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toOrderResponse(Order order) {
        List<OrderStageStatusResponse> stages = order.getStages().stream()
            .sorted(Comparator.comparingInt(status -> status.getStage().ordinal()))
            .map(this::toStageResponse)
            .collect(Collectors.toList());
        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getPriority(),
            order.getCurrentStage(),
            order.getOverallState(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            order.getNotes(),
            stages
        );
    }

    public OrderStageStatusResponse toStageResponse(OrderStageStatus status) {
        return new OrderStageStatusResponse(
            status.getId(),
            status.getStage(),
            status.getState(),
            status.getAssignee(),
            status.getClaimedAt(),
            status.getStartedAt(),
            status.getCompletedAt(),
            status.getServiceTimeMinutes(),
            status.getNotes(),
            status.getExceptionReason(),
            status.getSupervisorNotes(),
            status.getApprovedBy(),
            status.getUpdatedAt()
        );
    }

    public WorkQueueItemResponse toQueueItem(OrderStageStatus status) {
        Order order = status.getOrder();
        return new WorkQueueItemResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getPriority(),
            status.getStage(),
            status.getState(),
            order.getCurrentStage(),
            order.getOverallState(),
            status.getAssignee(),
            status.getClaimedAt(),
            status.getUpdatedAt(),
            status.getExceptionReason(),
            status.getNotes()
        );
    }
}
