package com.produsoft.workflow.controller;

import com.produsoft.workflow.dto.CreateOrderRequest;
import com.produsoft.workflow.dto.OrderMapper;
import com.produsoft.workflow.dto.OrderResponse;
import com.produsoft.workflow.dto.ReprioritizeOrderRequest;
import com.produsoft.workflow.service.OrderWorkflowService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderWorkflowService orderWorkflowService;
    private final OrderMapper mapper;

    public OrderController(OrderWorkflowService orderWorkflowService, OrderMapper mapper) {
        this.orderWorkflowService = orderWorkflowService;
        this.mapper = mapper;
    }

    @GetMapping
    public List<OrderResponse> listOrders() {
        return orderWorkflowService.findAllOrders().stream()
            .map(mapper::toOrderResponse)
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return mapper.toOrderResponse(orderWorkflowService.findOrder(id));
    }

    @PostMapping
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return mapper.toOrderResponse(orderWorkflowService.createOrder(request));
    }

    @PatchMapping("/{id}/priority")
    public OrderResponse updatePriority(@PathVariable Long id, @Valid @RequestBody ReprioritizeOrderRequest request) {
        return mapper.toOrderResponse(orderWorkflowService.updatePriority(id, request));
    }
}
