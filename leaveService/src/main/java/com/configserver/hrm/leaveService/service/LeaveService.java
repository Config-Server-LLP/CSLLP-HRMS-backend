package com.configserver.hrm.leaveService.service;

import com.configserver.hrm.leaveService.dto.LeaveRequestDTO;
import com.configserver.hrm.leaveService.dto.LeaveResponseDTO;
import com.configserver.hrm.leaveService.dto.LeaveBalanceDTO;
import com.configserver.hrm.leaveService.entity.EmployeeLeave;
import com.configserver.hrm.leaveService.entity.LeaveType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface LeaveService {

    EmployeeLeave applyLeave(String employeeId, String leaveType, LocalDate startDate, LocalDate endDate, String reason);
    EmployeeLeave approveLeave(UUID leaveId, String managerId) ;

    EmployeeLeave rejectLeave(UUID leaveId, String managerId);
    EmployeeLeave cancelLeave(UUID leaveId);
    List<EmployeeLeave> getLeavesByEmployee(String employeeId);
    List<EmployeeLeave> getAllLeaves();
     Map<LeaveType, Integer> getAllRemainingLeaves(String employeeId);
    void initializeAllEmployeesFromAttendance();
    List<EmployeeLeave> getPendingLeaves();
    List<EmployeeLeave> getLeavesBetweenDates(String employeeId, LocalDate from, LocalDate to);

}
