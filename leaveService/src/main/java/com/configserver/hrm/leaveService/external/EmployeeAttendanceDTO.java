package com.configserver.hrm.leaveService.external;

import java.time.LocalDate;

public class EmployeeAttendanceDTO {
    private String employeeId;
    private LocalDate date;
    private boolean present;

    // Getters & Setters
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }
}
