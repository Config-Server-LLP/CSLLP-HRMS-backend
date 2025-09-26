package com.configserver.hrm.leaveService.service.impl;

import com.configserver.hrm.leaveService.dto.HolidayDTO;
import com.configserver.hrm.leaveService.dto.LeaveRequestDTO;
import com.configserver.hrm.leaveService.dto.LeaveResponseDTO;
import com.configserver.hrm.leaveService.dto.LeaveBalanceDTO;
import com.configserver.hrm.leaveService.entity.EmployeeLeave;
import com.configserver.hrm.leaveService.entity.LeaveBalance;
import com.configserver.hrm.leaveService.entity.LeaveStatus;
import com.configserver.hrm.leaveService.entity.LeaveType;
import com.configserver.hrm.leaveService.exception.InvalidLeaveDataException;
import com.configserver.hrm.leaveService.exception.LeaveNotFoundException;
import com.configserver.hrm.leaveService.external.EmployeeAttendanceDTO;
import com.configserver.hrm.leaveService.repository.EmployeeLeaveRepository;
import com.configserver.hrm.leaveService.repository.LeaveBalanceRepository;
import com.configserver.hrm.leaveService.service.HolidayService;
import com.configserver.hrm.leaveService.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaveServiceImpl implements LeaveService {
    @Value("${hrm.holidays}")
    private String holidaysConfig;
    @Autowired
    private EmployeeLeaveRepository leaveRepository;
    @Autowired
    HolidayService holidayService;

    @Autowired
    private LeaveBalanceRepository balanceRepository;

    @Autowired
    private RestTemplate restTemplate;

    private static final String ATTENDANCE_SERVICE_URL = "http://localhost:8085/api/attendance";

    @Override
    @Transactional
    public EmployeeLeave applyLeave(String employeeId, String leaveType, LocalDate startDate, LocalDate endDate, String reason) {

        // Step 1: Ensure leave balances exist for this employee
        try {
            initializeLeaveBalances(employeeId);
        } catch (Exception e) {
            System.err.println("Warning: Failed to initialize leave balances for employee " + employeeId + ": " + e.getMessage());
        }

        // Step 2: Validate dates
        if (endDate.isBefore(startDate)) {
            throw new InvalidLeaveDataException("End date cannot be before start date");
        }

        List<HolidayDTO> holidayList = holidayService.getHolidays();
        Set<LocalDate> holidays = holidayList.stream()
                .map(HolidayDTO::getDate)
                .collect(Collectors.toSet());

        boolean hasHoliday = startDate.datesUntil(endDate.plusDays(1))
                .anyMatch(holidays::contains);

        if (hasHoliday) {
            throw new InvalidLeaveDataException("Requested leave includes a holiday between " + startDate + " and " + endDate);
        }

        // Step 4: Get leave balance
        LeaveBalance balance = balanceRepository.findByEmployeeIdAndLeaveType(employeeId, LeaveType.valueOf(leaveType))
                .orElseThrow(() -> new InvalidLeaveDataException("No leave balance found for type: " + leaveType));

        // Step 5: Calculate requested days
        long daysRequested = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        if (daysRequested > balance.getRemainingLeaves()) {
            throw new InvalidLeaveDataException("Insufficient leave balance. Remaining: " + balance.getRemainingLeaves());
        }

        // Step 6: Create leave request (PENDING status)
        EmployeeLeave leave = new EmployeeLeave();
        leave.setEmployeeId(employeeId);
        leave.setLeaveType(LeaveType.valueOf(leaveType));
        leave.setStartDate(startDate);
        leave.setEndDate(endDate);
        leave.setReason(reason);
        leave.setStatus(LeaveStatus.PENDING);
        leave.setAppliedOn(LocalDateTime.now());

        try {
            leaveRepository.save(leave);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save leave request: " + e.getMessage());
        }

        return leave;
    }

    @Override
    @Transactional
    public EmployeeLeave approveLeave(UUID leaveId, String managerId) {

        EmployeeLeave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException("Only pending leaves can be approved");
        }

        // Update leave status
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApprovedBy(managerId);
        leave.setApprovedOn(LocalDateTime.now());
        leaveRepository.save(leave);

        // Deduct leave balance only for paid leave types
        if (leave.getLeaveType() != LeaveType.UNPAID) {
            LeaveBalance balance = balanceRepository
                    .findByEmployeeIdAndLeaveType(leave.getEmployeeId(), leave.getLeaveType())
                    .orElseThrow(() -> new RuntimeException("Leave balance not found"));

            long days = leave.getEndDate().toEpochDay() - leave.getStartDate().toEpochDay() + 1;
            balance.setUsedLeaves(balance.getUsedLeaves() + (int) days);
            balance.setRemainingLeaves(balance.getRemainingLeaves() - (int) days);
            balanceRepository.save(balance);
        } else {
            System.out.println("Unpaid leave approved for employee " + leave.getEmployeeId() +
                    ". Salary should be adjusted accordingly.");
        }

        return leave;
    }

    @Override
    @Transactional
    public EmployeeLeave rejectLeave(UUID leaveId, String managerId) {
        EmployeeLeave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException("Only pending leaves can be rejected");
        }

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setApprovedBy(managerId);
        leave.setApprovedOn(LocalDateTime.now());

        return leaveRepository.save(leave);
    }


    @Override
    @Transactional
    public EmployeeLeave cancelLeave(UUID leaveId) {
        EmployeeLeave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getStatus() != LeaveStatus.PENDING && leave.getStatus() != LeaveStatus.APPROVED) {
            throw new RuntimeException("Only pending or approved leaves can be cancelled");
        }

        // If leave was already approved, refund balance
        if (leave.getStatus() == LeaveStatus.APPROVED) {
            LeaveBalance balance = balanceRepository
                    .findByEmployeeIdAndLeaveType(leave.getEmployeeId(), leave.getLeaveType())
                    .orElseThrow(() -> new RuntimeException("Leave balance not found"));

            long days = leave.getEndDate().toEpochDay() - leave.getStartDate().toEpochDay() + 1;
            balance.setUsedLeaves(balance.getUsedLeaves() - (int) days);
            balance.setRemainingLeaves(balance.getRemainingLeaves() + (int) days);
            balanceRepository.save(balance);
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        return leaveRepository.save(leave);
    }

    public List<EmployeeLeave> getLeavesByEmployee(String employeeId) {
        return leaveRepository.findByEmployeeId(employeeId);
    }

    public List<EmployeeLeave> getPendingLeaves() {
        return leaveRepository.findByStatus(LeaveStatus.valueOf("PENDING"));
    }


    public List<EmployeeLeave> getAllLeaves() {
        return leaveRepository.findAll();
    }

    public Map<LeaveType, Integer> getAllRemainingLeaves(String employeeId) {
        List<LeaveBalance> balances = balanceRepository.findByEmployeeId(employeeId);

        if (balances.isEmpty()) {
            throw new RuntimeException("Leave balances not found for employee: " + employeeId);
        }

        // Convert list to a map of LeaveType -> remainingLeaves
        return balances.stream()
                .collect(Collectors.toMap(LeaveBalance::getLeaveType, LeaveBalance::getRemainingLeaves));
    }



    // Fetch today's attendance (optional)
    public Object getTodayAttendance() {
        String url = ATTENDANCE_SERVICE_URL + "/import/daily";
        ResponseEntity<Object> response = restTemplate.postForEntity(url, null, Object.class);
        return response.getBody();
    }

    private void initializeLeaveBalances(String employeeId) {
        String[] leaveTypes = {"SICK", "PAID", "CASUAL"};

        for (String type : leaveTypes) {
            try {
                balanceRepository.findByEmployeeIdAndLeaveType(employeeId, LeaveType.valueOf(type))
                        .orElseGet(() -> {
                            LeaveBalance balance = new LeaveBalance();
                            balance.setEmployeeId(employeeId);
                            balance.setLeaveType(LeaveType.valueOf(type));
                            // Default leave counts
                            switch (type) {
                                case "SICK" -> balance.setTotalLeaves(10);
                                case "PAID" -> balance.setTotalLeaves(15);
                                case "CASUAL" -> balance.setTotalLeaves(7);
                            }
                            balance.setUsedLeaves(0);
                            balance.setRemainingLeaves(balance.getTotalLeaves());
                            return balanceRepository.save(balance);
                        });
            } catch (Exception e) {
                System.err.println("Failed to initialize leave balance for employee " + employeeId +
                        " and leave type " + type + ": " + e.getMessage());
            }
        }
    }

    // Optional: initialize all employees from attendance service
    @Override
    public void initializeAllEmployeesFromAttendance() {
        String url = ATTENDANCE_SERVICE_URL + "/import/daily";
        try {
            ResponseEntity<List<EmployeeAttendanceDTO>> response = restTemplate.exchange(
                    url, HttpMethod.POST, null, new ParameterizedTypeReference<List<EmployeeAttendanceDTO>>() {
                    }
            );
            List<EmployeeAttendanceDTO> employees = response.getBody();
            if (employees != null) {
                for (EmployeeAttendanceDTO emp : employees) {
                    initializeLeaveBalances(emp.getEmployeeId());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch employees from Attendance Service: " + e.getMessage());
        }
    }

    private Set<LocalDate> getHolidays() {
        return Arrays.stream(holidaysConfig.split(","))
                .map(LocalDate::parse)
                .collect(Collectors.toSet());
    }


    private LeaveResponseDTO mapToDTO(EmployeeLeave leave) {
        LeaveResponseDTO dto = new LeaveResponseDTO();
        dto.setId(String.valueOf(leave.getId()));
        dto.setEmployeeId(leave.getEmployeeId());
        dto.setLeaveType(String.valueOf(leave.getLeaveType()));
        dto.setStartDate(leave.getStartDate());
        dto.setEndDate(leave.getEndDate());
        dto.setStatus(leave.getStatus());
        dto.setReason(leave.getReason());
        dto.setApprovedBy(leave.getApprovedBy());
        dto.setAppliedOn(leave.getAppliedOn());
        dto.setApprovedOn(leave.getApprovedOn());
        return dto;
    }
    @Override
    public List<EmployeeLeave> getLeavesBetweenDates(String employeeId, LocalDate from, LocalDate to) {
        return leaveRepository.findByEmployeeIdAndStartDateBetween(employeeId, from, to);
    }


}