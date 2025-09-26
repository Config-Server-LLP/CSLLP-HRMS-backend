package com.configserver.hrm.leaveService.dto;

public class LeaveBalanceDTO {

    private String employeeId;
    private String leaveType;
    private int totalLeaves;
    private int usedLeaves;
    private int remainingLeaves;

    // Getters & Setters
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public int getTotalLeaves() { return totalLeaves; }
    public void setTotalLeaves(int totalLeaves) { this.totalLeaves = totalLeaves; }

    public int getUsedLeaves() { return usedLeaves; }
    public void setUsedLeaves(int usedLeaves) { this.usedLeaves = usedLeaves; }

    public int getRemainingLeaves() { return remainingLeaves; }
    public void setRemainingLeaves(int remainingLeaves) { this.remainingLeaves = remainingLeaves; }
}
