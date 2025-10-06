package com.produsoft.workflow.config;

import com.produsoft.workflow.domain.StageType;
import com.produsoft.workflow.dto.CompleteStageRequest;
import com.produsoft.workflow.dto.CreateOrderRequest;
import com.produsoft.workflow.dto.FlagStageExceptionRequest;
import com.produsoft.workflow.dto.SupervisorDecisionRequest;
import com.produsoft.workflow.service.OrderWorkflowService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedOrders(OrderWorkflowService workflowService) {
        return args -> {
            if (!workflowService.findAllOrders().isEmpty()) {
                return;
            }

            var order1 = workflowService.createOrder(new CreateOrderRequest("PO-1001", 3, "Client A first batch"));
            workflowService.claimStage(order1.getId(), StageType.PREPARATION, "operator1");
            workflowService.completeStage(order1.getId(), StageType.PREPARATION, new CompleteStageRequest("operator1", 30L, "Prep done"));
            workflowService.claimStage(order1.getId(), StageType.ASSEMBLY, "operator2");
            workflowService.completeStage(order1.getId(), StageType.ASSEMBLY, new CompleteStageRequest("operator2", 55L, "Assembly initial pass"));

            var order2 = workflowService.createOrder(new CreateOrderRequest("PO-1002", 5, "Urgent order"));
            workflowService.claimStage(order2.getId(), StageType.PREPARATION, "operator1");
            workflowService.completeStage(order2.getId(), StageType.PREPARATION, new CompleteStageRequest("operator1", 20L, "Fast prep"));
            workflowService.claimStage(order2.getId(), StageType.ASSEMBLY, "operator2");
            workflowService.flagException(order2.getId(), StageType.ASSEMBLY, new FlagStageExceptionRequest("operator2", "Missing components", "Waiting on supplier"));

            var order3 = workflowService.createOrder(new CreateOrderRequest("PO-1003", 2, "Standard run"));
            workflowService.claimStage(order3.getId(), StageType.PREPARATION, "operator3");
            workflowService.completeStage(order3.getId(), StageType.PREPARATION, new CompleteStageRequest("operator3", 40L, "Long prep"));
            workflowService.claimStage(order3.getId(), StageType.ASSEMBLY, "operator4");
            workflowService.completeStage(order3.getId(), StageType.ASSEMBLY, new CompleteStageRequest("operator4", 50L, "Assembly done"));
            workflowService.claimStage(order3.getId(), StageType.DELIVERY, "operator5");
            workflowService.completeStage(order3.getId(), StageType.DELIVERY, new CompleteStageRequest("operator5", 15L, "Delivered"));

            workflowService.approveSkip(order2.getId(), StageType.ASSEMBLY, new SupervisorDecisionRequest("supervisor1", "Approve skip due to parts shortage"));
            workflowService.requestRework(order1.getId(), StageType.ASSEMBLY, new SupervisorDecisionRequest("supervisor1", "Quality issue found"));
        };
    }
}
