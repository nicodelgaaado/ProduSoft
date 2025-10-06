package com.produsoft.workflow.service;

import com.produsoft.workflow.domain.Order;
import com.produsoft.workflow.domain.OrderStageStatus;
import com.produsoft.workflow.domain.StageState;
import com.produsoft.workflow.domain.StageType;
import com.produsoft.workflow.dto.CompleteStageRequest;
import com.produsoft.workflow.dto.CreateOrderRequest;
import com.produsoft.workflow.dto.FlagStageExceptionRequest;
import com.produsoft.workflow.dto.ReprioritizeOrderRequest;
import com.produsoft.workflow.dto.StageSummaryResponse;
import com.produsoft.workflow.dto.SupervisorDecisionRequest;
import com.produsoft.workflow.dto.WipSummaryResponse;
import com.produsoft.workflow.exception.InvalidStageActionException;
import com.produsoft.workflow.exception.NotFoundException;
import com.produsoft.workflow.repository.OrderRepository;
import com.produsoft.workflow.repository.OrderStageStatusRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class OrderWorkflowService {

    private final OrderRepository orderRepository;
    private final OrderStageStatusRepository stageStatusRepository;

    public OrderWorkflowService(OrderRepository orderRepository,
                                OrderStageStatusRepository stageStatusRepository) {
        this.orderRepository = orderRepository;
        this.stageStatusRepository = stageStatusRepository;
    }

    public Order createOrder(CreateOrderRequest request) {
        orderRepository.findByOrderNumber(request.orderNumber())
            .ifPresent(existing -> {
                throw new InvalidStageActionException("Order number already exists: " + request.orderNumber());
            });

        Instant now = Instant.now();
        Order order = new Order();
        order.setOrderNumber(request.orderNumber());
        order.setPriority(request.priority());
        order.setNotes(request.notes());
        order.setCurrentStage(StageType.PREPARATION);
        order.setOverallState(StageState.PENDING);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        for (StageType stage : StageType.values()) {
            OrderStageStatus status = new OrderStageStatus();
            status.setStage(stage);
            status.setUpdatedAt(now);
            if (stage == StageType.PREPARATION) {
                status.markPending();
            } else {
                status.markBlocked();
            }
            order.addStageStatus(status);
        }

        order.getStages().sort(Comparator.comparingInt(s -> s.getStage().ordinal()));
        return orderRepository.save(order);
    }

    public List<Order> findAllOrders() {
        return orderRepository.findAll(Sort.by(Sort.Order.desc("priority"), Sort.Order.asc("createdAt")));
    }

    public Order findOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }

    public List<OrderStageStatus> fetchQueue(StageType stage, List<StageState> states) {
        List<StageState> effectiveStates = (states == null || states.isEmpty())
            ? List.of(StageState.PENDING, StageState.REWORK)
            : states;
        return stageStatusRepository.findQueueByStageAndStates(stage, effectiveStates);
    }

    public OrderStageStatus claimStage(Long orderId, StageType stage, String assignee) {
        OrderStageStatus status = getStageStatus(orderId, stage);
        if (!(status.getState() == StageState.PENDING || status.getState() == StageState.REWORK)) {
            throw new InvalidStageActionException("Stage is not available to claim: " + stage);
        }
        status.markInProgress(assignee);
        Order order = status.getOrder();
        order.setCurrentStage(stage);
        order.touch();
        order.setOverallState(StageState.IN_PROGRESS);
        return status;
    }

    public OrderStageStatus completeStage(Long orderId, StageType stage, CompleteStageRequest request) {
        OrderStageStatus status = getStageStatus(orderId, stage);
        if (status.getState() != StageState.IN_PROGRESS) {
            throw new InvalidStageActionException("Stage must be in progress to complete: " + stage);
        }
        status.setAssignee(request.assignee());
        status.markCompleted(request.serviceTimeMinutes(), request.notes());
        Order order = status.getOrder();
        order.touch();
        advanceToNextStage(order, stage);
        return status;
    }

    public OrderStageStatus flagException(Long orderId, StageType stage, FlagStageExceptionRequest request) {
        OrderStageStatus status = getStageStatus(orderId, stage);
        EnumSet<StageState> allowed = EnumSet.of(StageState.IN_PROGRESS, StageState.PENDING, StageState.REWORK);
        if (!allowed.contains(status.getState())) {
            throw new InvalidStageActionException("Cannot flag exception for stage in state " + status.getState());
        }
        status.setAssignee(request.assignee());
        status.markException(request.exceptionReason(), request.notes());
        Order order = status.getOrder();
        order.setOverallState(StageState.EXCEPTION);
        order.touch();
        return status;
    }

    public OrderStageStatus approveSkip(Long orderId, StageType stage, SupervisorDecisionRequest request) {
        OrderStageStatus status = getStageStatus(orderId, stage);
        if (status.getState() != StageState.EXCEPTION && status.getState() != StageState.PENDING) {
            throw new InvalidStageActionException("Only exception or pending stages can be skipped");
        }
        status.markSkipped(request.notes(), request.approver());
        Order order = status.getOrder();
        order.touch();
        advanceToNextStage(order, stage);
        return status;
    }

    public OrderStageStatus requestRework(Long orderId, StageType stage, SupervisorDecisionRequest request) {
        OrderStageStatus status = getStageStatus(orderId, stage);
        if (status.getState() != StageState.COMPLETED && status.getState() != StageState.EXCEPTION) {
            throw new InvalidStageActionException("Rework can only be requested on completed or exception stages");
        }
        status.markRework(request.notes(), request.approver());
        Order order = status.getOrder();
        resetDownstreamStages(order, stage);
        order.setCurrentStage(stage);
        order.setOverallState(StageState.IN_PROGRESS);
        order.touch();
        return status;
    }

    public Order updatePriority(Long orderId, ReprioritizeOrderRequest request) {
        Order order = findOrder(orderId);
        order.setPriority(request.priority());
        order.touch();
        return order;
    }

    public void advanceToNextStage(Order order, StageType currentStage) {
        Optional<StageType> nextStage = currentStage.next();
        if (nextStage.isPresent()) {
            OrderStageStatus nextStatus = order.getStageStatus(nextStage.get())
                .orElseThrow(() -> new IllegalStateException("Missing stage status for " + nextStage.get()));
            if (nextStatus.getState() == StageState.BLOCKED || nextStatus.getState() == StageState.REWORK) {
                nextStatus.markPending();
            }
            order.setCurrentStage(nextStage.get());
            order.setOverallState(StageState.IN_PROGRESS);
        } else {
            order.setOverallState(StageState.COMPLETED);
            order.setCurrentStage(currentStage);
        }
        updateOrderState(order);
    }

    public List<OrderStageStatus> findStagesForOrder(Long orderId) {
        return new ArrayList<>(stageStatusRepository.findByOrderId(orderId));
    }

    public WipSummaryResponse buildWipSummary() {
        List<Order> orders = orderRepository.findAll();
        long total = orders.size();
        long completedOrders = orders.stream()
            .filter(order -> order.getOverallState() == StageState.COMPLETED)
            .count();
        long exceptionOrders = orders.stream()
            .filter(order -> order.getOverallState() == StageState.EXCEPTION)
            .count();

        List<StageSummaryResponse> stageSummaries = Arrays.stream(StageType.values())
            .map(stage -> {
                long pending = orders.stream()
                    .map(order -> order.getStageStatus(stage).orElse(null))
                    .filter(Objects::nonNull)
                    .filter(status -> status.getState() == StageState.PENDING || status.getState() == StageState.REWORK)
                    .count();
                long inProgress = orders.stream()
                    .map(order -> order.getStageStatus(stage).orElse(null))
                    .filter(Objects::nonNull)
                    .filter(status -> status.getState() == StageState.IN_PROGRESS)
                    .count();
                long exceptions = orders.stream()
                    .map(order -> order.getStageStatus(stage).orElse(null))
                    .filter(Objects::nonNull)
                    .filter(status -> status.getState() == StageState.EXCEPTION)
                    .count();
                long stageCompleted = orders.stream()
                    .map(order -> order.getStageStatus(stage).orElse(null))
                    .filter(Objects::nonNull)
                    .filter(status -> status.getState() == StageState.COMPLETED || status.getState() == StageState.SKIPPED)
                    .count();
                return new StageSummaryResponse(stage, pending, inProgress, exceptions, stageCompleted);
            })
            .collect(Collectors.toList());

        return new WipSummaryResponse(total, completedOrders, exceptionOrders, stageSummaries);
    }

    private void updateOrderState(Order order) {
        boolean hasException = order.getStages().stream().anyMatch(s -> s.getState() == StageState.EXCEPTION);
        if (hasException) {
            order.setOverallState(StageState.EXCEPTION);
            return;
        }
        boolean allCompleted = order.getStages().stream()
            .allMatch(s -> s.getState() == StageState.COMPLETED || s.getState() == StageState.SKIPPED);
        if (allCompleted) {
            order.setOverallState(StageState.COMPLETED);
            return;
        }
        boolean anyInProgress = order.getStages().stream().anyMatch(s -> s.getState() == StageState.IN_PROGRESS);
        if (anyInProgress) {
            order.setOverallState(StageState.IN_PROGRESS);
            return;
        }
        order.setOverallState(StageState.PENDING);
    }

    private void resetDownstreamStages(Order order, StageType stage) {
        for (OrderStageStatus status : order.getStages()) {
            if (status.getStage().ordinal() > stage.ordinal()) {
                status.markBlocked();
                status.setAssignee(null);
                status.setClaimedAt(null);
                status.setStartedAt(null);
                status.setCompletedAt(null);
                status.setServiceTimeMinutes(null);
                status.setNotes(null);
                status.setExceptionReason(null);
                status.setSupervisorNotes(null);
                status.setApprovedBy(null);
            }
        }
    }

    private OrderStageStatus getStageStatus(Long orderId, StageType stage) {
        return stageStatusRepository.findByOrderIdAndStage(orderId, stage)
            .orElseThrow(() -> new NotFoundException("Stage status not found for order %d and stage %s".formatted(orderId, stage)));
    }
}
