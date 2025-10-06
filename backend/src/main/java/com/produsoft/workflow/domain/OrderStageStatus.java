package com.produsoft.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "order_stage_status")
public class OrderStageStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    private StageType stage;

    @Enumerated(EnumType.STRING)
    private StageState state;

    private String assignee;

    private Instant claimedAt;

    private Instant startedAt;

    private Instant completedAt;

    private Long serviceTimeMinutes;

    @Column(length = 1024)
    private String notes;

    @Column(length = 1024)
    private String exceptionReason;

    @Column(length = 1024)
    private String supervisorNotes;

    private String approvedBy;

    private Instant updatedAt;

    public void markPending() {
        this.state = StageState.PENDING;
        this.updatedAt = Instant.now();
    }

    public void markBlocked() {
        this.state = StageState.BLOCKED;
        this.updatedAt = Instant.now();
    }

    public void markInProgress(String assignee) {
        this.assignee = assignee;
        Instant now = Instant.now();
        this.claimedAt = now;
        this.startedAt = now;
        this.state = StageState.IN_PROGRESS;
        this.updatedAt = now;
    }

    public void markCompleted(Long serviceTimeMinutes, String notes) {
        Instant now = Instant.now();
        this.completedAt = now;
        if (serviceTimeMinutes == null && startedAt != null) {
            this.serviceTimeMinutes = Duration.between(startedAt, now).toMinutes();
        } else {
            this.serviceTimeMinutes = serviceTimeMinutes;
        }
        this.notes = notes;
        this.state = StageState.COMPLETED;
        this.updatedAt = now;
    }

    public void markException(String exceptionReason, String notes) {
        this.exceptionReason = exceptionReason;
        this.notes = notes;
        this.state = StageState.EXCEPTION;
        this.updatedAt = Instant.now();
    }

    public void markSkipped(String supervisorNotes, String approvedBy) {
        Instant now = Instant.now();
        this.supervisorNotes = supervisorNotes;
        this.approvedBy = approvedBy;
        this.completedAt = now;
        this.state = StageState.SKIPPED;
        this.updatedAt = now;
    }

    public void markRework(String supervisorNotes, String approvedBy) {
        this.supervisorNotes = supervisorNotes;
        this.approvedBy = approvedBy;
        this.assignee = null;
        this.claimedAt = null;
        this.startedAt = null;
        this.completedAt = null;
        this.serviceTimeMinutes = null;
        this.notes = null;
        this.exceptionReason = null;
        this.state = StageState.REWORK;
        this.updatedAt = Instant.now();
    }

    public void markReadyAfterRework() {
        this.state = StageState.PENDING;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public StageType getStage() {
        return stage;
    }

    public void setStage(StageType stage) {
        this.stage = stage;
    }

    public StageState getState() {
        return state;
    }

    public void setState(StageState state) {
        this.state = state;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Instant claimedAt) {
        this.claimedAt = claimedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Long getServiceTimeMinutes() {
        return serviceTimeMinutes;
    }

    public void setServiceTimeMinutes(Long serviceTimeMinutes) {
        this.serviceTimeMinutes = serviceTimeMinutes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getExceptionReason() {
        return exceptionReason;
    }

    public void setExceptionReason(String exceptionReason) {
        this.exceptionReason = exceptionReason;
    }

    public String getSupervisorNotes() {
        return supervisorNotes;
    }

    public void setSupervisorNotes(String supervisorNotes) {
        this.supervisorNotes = supervisorNotes;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderStageStatus that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
