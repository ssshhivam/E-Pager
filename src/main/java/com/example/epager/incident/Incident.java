package com.example.epager.incident;

import com.example.epager.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String externalAlertId;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status = IncidentStatus.TRIGGERED;

    private Integer currentEscalationLevel = 0;

    private LocalDateTime nextEscalationAt;

    @ManyToOne(fetch = FetchType.EAGER)
    private AppUser assignedUser;

    @ManyToOne(fetch = FetchType.EAGER)
    private AppUser acknowledgedBy;

    private LocalDateTime createdAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalAlertId() {
        return externalAlertId;
    }

    public void setExternalAlertId(String externalAlertId) {
        this.externalAlertId = externalAlertId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public Integer getCurrentEscalationLevel() {
        return currentEscalationLevel;
    }

    public void setCurrentEscalationLevel(Integer currentEscalationLevel) {
        this.currentEscalationLevel = currentEscalationLevel;
    }

    public LocalDateTime getNextEscalationAt() {
        return nextEscalationAt;
    }

    public void setNextEscalationAt(LocalDateTime nextEscalationAt) {
        this.nextEscalationAt = nextEscalationAt;
    }

    public AppUser getAssignedUser() {
        return assignedUser;
    }

    public void setAssignedUser(AppUser assignedUser) {
        this.assignedUser = assignedUser;
    }

    public AppUser getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(AppUser acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
