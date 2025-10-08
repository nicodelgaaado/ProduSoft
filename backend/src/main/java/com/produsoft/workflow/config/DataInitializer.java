package com.produsoft.workflow.config;

import com.produsoft.workflow.checklist.StageChecklistService;
import com.produsoft.workflow.domain.StageType;
import com.produsoft.workflow.dto.CompleteStageRequest;
import com.produsoft.workflow.dto.CreateOrderRequest;
import com.produsoft.workflow.dto.UpdateChecklistItemRequest;
import com.produsoft.workflow.service.OrderWorkflowService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedOrders(OrderWorkflowService workflowService, StageChecklistService checklistService) {
        return args -> {
            if (!workflowService.findAllOrders().isEmpty()) {
                return;
            }

            workflowService.createOrder(new CreateOrderRequest("PO-1001", 3, "Client A first batch"));

            var order2 = workflowService.createOrder(new CreateOrderRequest("PO-1002", 5, "Urgent order"));
            workflowService.claimStage(order2.getId(), StageType.PREPARATION, "operator1");
            markChecklistComplete(workflowService, checklistService, order2.getId(), StageType.PREPARATION);
            workflowService.completeStage(order2.getId(), StageType.PREPARATION, new CompleteStageRequest("operator1", 20L, "Fast prep"));

            var order3 = workflowService.createOrder(new CreateOrderRequest("PO-1003", 2, "Standard run"));
            workflowService.claimStage(order3.getId(), StageType.PREPARATION, "operator3");
            markChecklistComplete(workflowService, checklistService, order3.getId(), StageType.PREPARATION);
            workflowService.completeStage(order3.getId(), StageType.PREPARATION, new CompleteStageRequest("operator3", 40L, "Long prep"));
            workflowService.claimStage(order3.getId(), StageType.ASSEMBLY, "operator4");
            markChecklistComplete(workflowService, checklistService, order3.getId(), StageType.ASSEMBLY);
            workflowService.completeStage(order3.getId(), StageType.ASSEMBLY, new CompleteStageRequest("operator4", 50L, "Assembly done"));
        };
    }

    private void markChecklistComplete(OrderWorkflowService workflowService,
                                       StageChecklistService checklistService,
                                       Long orderId,
                                       StageType stage) {
        checklistService.definitionsFor(stage).forEach(task ->
            workflowService.updateChecklistItem(orderId, stage, new UpdateChecklistItemRequest(task.id(), true))
        );
    }
}
