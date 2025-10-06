package com.produsoft.workflow.repository;

import com.produsoft.workflow.domain.OrderStageStatus;
import com.produsoft.workflow.domain.StageState;
import com.produsoft.workflow.domain.StageType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderStageStatusRepository extends JpaRepository<OrderStageStatus, Long> {

    @Query("SELECT oss FROM OrderStageStatus oss JOIN FETCH oss.order o " +
           "WHERE oss.stage = :stage AND oss.state IN :states " +
           "ORDER BY COALESCE(o.priority, 0) DESC, o.createdAt ASC")
    List<OrderStageStatus> findQueueByStageAndStates(@Param("stage") StageType stage,
                                                     @Param("states") Collection<StageState> states);

    @Query("SELECT oss FROM OrderStageStatus oss JOIN FETCH oss.order o WHERE o.id = :orderId AND oss.stage = :stage")
    Optional<OrderStageStatus> findByOrderIdAndStage(@Param("orderId") Long orderId, @Param("stage") StageType stage);

    List<OrderStageStatus> findByOrderId(Long orderId);
}
