package com.configserver.hrm.leaveService.dto;

import com.configserver.hrm.leaveService.entity.LeaveStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LeaveResponseDTO {

    private String id;
    private String employeeId;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LeaveStatus status;
    private String reason;
    private String approvedBy;
    private LocalDateTime appliedOn;
    private LocalDateTime approvedOn;

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LeaveStatus getStatus() { return status; }
    public void setStatus(LeaveStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getAppliedOn() { return appliedOn; }
    public void setAppliedOn(LocalDateTime appliedOn) { this.appliedOn = appliedOn; }

    public LocalDateTime getApprovedOn() { return approvedOn; }
    public void setApprovedOn(LocalDateTime approvedOn) { this.approvedOn = approvedOn; }
}
