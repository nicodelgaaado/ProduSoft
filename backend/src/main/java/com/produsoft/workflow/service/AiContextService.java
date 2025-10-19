package com.produsoft.workflow.service;

import com.produsoft.workflow.domain.Order;
import com.produsoft.workflow.domain.OrderStageStatus;
import com.produsoft.workflow.domain.StageState;
import com.produsoft.workflow.domain.StageType;
import com.produsoft.workflow.dto.AiChatRequest;
import com.produsoft.workflow.repository.OrderRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AiContextService {

    private static final int MAX_ORDERS = 25;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm 'UTC'")
        .withZone(ZoneOffset.UTC);

    private final OrderRepository orderRepository;

    public AiContextService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public AiChatRequest.Message buildContextMessage() {
        return new AiChatRequest.Message("system", buildContext());
    }

    private String buildContext() {
        List<Order> orders = orderRepository.findAll();
        StringBuilder builder = new StringBuilder();
        builder.append("You are ProduSoft's workflow assistant. Use only the data below to answer operations questions. ")
            .append("If something is unknown in the data, say so. ")
            .append("Current timestamp: ")
            .append(TIMESTAMP_FORMATTER.format(Instant.now()))
            .append(".\n\n");

        if (orders.isEmpty()) {
            builder.append("No orders currently exist in the system.");
            return builder.toString();
        }

        builder.append("Total orders: ").append(orders.size()).append(". ")
            .append("Showing up to ").append(Math.min(MAX_ORDERS, orders.size()))
            .append(" most recent by priority and creation time.\n");

        orders.stream()
            .sorted(Comparator
                .comparing((Order order) -> Optional.ofNullable(order.getPriority()).orElse(0), Comparator.reverseOrder())
                .thenComparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(MAX_ORDERS)
            .forEach(order -> appendOrderSummary(builder, order));

        return builder.toString();
    }

    private void appendOrderSummary(StringBuilder builder, Order order) {
        builder.append("\nOrder ").append(Optional.ofNullable(order.getOrderNumber()).orElse("unknown"))
            .append(" (id=").append(order.getId()).append(")");
        if (order.getPriority() != null) {
            builder.append(" priority ").append(order.getPriority());
        }
        builder.append("; overall state ").append(nullableState(order.getOverallState()))
            .append("; current stage ").append(nullableStage(order.getCurrentStage())).append(".");
        if (order.getNotes() != null && !order.getNotes().isBlank()) {
            builder.append(" Notes: ").append(order.getNotes());
        }
        builder.append("\n  Stages:");

        order.getStages().stream()
            .sorted(Comparator.comparing(status -> status.getStage().ordinal()))
            .forEach(status -> appendStageSummary(builder, status));
    }

    private void appendStageSummary(StringBuilder builder, OrderStageStatus status) {
        builder.append("\n    - ").append(status.getStage().name().toLowerCase(Locale.ROOT))
            .append(": ").append(status.getState().name().toLowerCase(Locale.ROOT));
        if (status.getAssignee() != null) {
            builder.append(" (assignee ").append(status.getAssignee()).append(")");
        }
        if (status.getExceptionReason() != null && !status.getExceptionReason().isBlank()) {
            builder.append(" exception=").append(status.getExceptionReason());
        }
        if (status.getSupervisorNotes() != null && !status.getSupervisorNotes().isBlank()) {
            builder.append(" supervisorNotes=").append(status.getSupervisorNotes());
        }
        if (status.getNotes() != null && !status.getNotes().isBlank()) {
            builder.append(" notes=").append(status.getNotes());
        }
    }

    private String nullableStage(StageType stageType) {
        return stageType == null ? "unknown" : stageType.name().toLowerCase(Locale.ROOT);
    }

    private String nullableState(StageState state) {
        return state == null ? "unknown" : state.name().toLowerCase(Locale.ROOT);
    }
}
