package com.configserver.hrm.leaveService.controller;

import com.configserver.hrm.leaveService.dto.LeaveRequestDTO;
import com.configserver.hrm.leaveService.dto.LeaveResponseDTO;
import com.configserver.hrm.leaveService.dto.LeaveBalanceDTO;
import com.configserver.hrm.leaveService.entity.EmployeeLeave;
import com.configserver.hrm.leaveService.entity.LeaveType;
import com.configserver.hrm.leaveService.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @PostMapping("/apply")
    public ResponseEntity<?> applyLeave(@RequestBody EmployeeLeave leaveRequest) {
        try {
            String employeeId = leaveRequest.getEmployeeId();
            String leaveType = String.valueOf(leaveRequest.getLeaveType());
            LocalDate startDate = leaveRequest.getStartDate();
            LocalDate endDate = leaveRequest.getEndDate();
            String reason = leaveRequest.getReason();

            EmployeeLeave leave = leaveService.applyLeave(employeeId, leaveType, startDate, endDate, reason);
            return new ResponseEntity<>(leave, HttpStatus.CREATED);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + ex.getMessage());
        }
    }


        @PutMapping("/approve/{leaveId}")
        public ResponseEntity<?> approveLeave(
                @PathVariable String leaveId,
                @RequestParam String managerId) {
            try {
                // Convert String to UUID
                UUID leaveUuid;
                try {
                    leaveUuid = UUID.fromString(leaveId);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body("Invalid leave ID format: " + leaveId);
                }

                EmployeeLeave approvedLeave = leaveService.approveLeave(leaveUuid, managerId);
                return ResponseEntity.ok(approvedLeave);

            } catch (RuntimeException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Error: " + ex.getMessage());
            }
        }



    @PutMapping("/reject/{leaveId}")
    public ResponseEntity<?> rejectLeave(@PathVariable String leaveId, @RequestParam String managerId) {
        try {
            UUID leaveUuid = UUID.fromString(leaveId); // convert string to UUID

            EmployeeLeave rejectedLeave = leaveService.rejectLeave(leaveUuid, managerId);
            return ResponseEntity.ok(rejectedLeave);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + ex.getMessage());
        }
    }

    @PutMapping("/cancel/{leaveId}")
    public ResponseEntity<?> cancelLeave(@PathVariable String leaveId) {
        try {
            UUID leaveUuid = UUID.fromString(leaveId); // convert string to UUID

            EmployeeLeave cancelledLeave = leaveService.cancelLeave(leaveUuid);
            return ResponseEntity.ok(cancelledLeave);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + ex.getMessage());
        }
    }


    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EmployeeLeave>> getLeavesByEmployee(@PathVariable String employeeId) {
      //  UUID leaveUuid = UUID.fromString(employeeId); // convert string to UUID

        List<EmployeeLeave> leaves = leaveService.getLeavesByEmployee(employeeId);
        return ResponseEntity.ok(leaves);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<EmployeeLeave>> getPendingLeaves() {
        return ResponseEntity.ok(leaveService.getPendingLeaves());
    }

    @GetMapping("/all")
    public ResponseEntity<List<EmployeeLeave>> getAllLeaves() {
        return ResponseEntity.ok(leaveService.getAllLeaves());
    }


    @GetMapping("/leave-balance/{employeeId}")
    public ResponseEntity<?> getLeaveBalances(@PathVariable String employeeId) {
        try {
            Map<LeaveType, Integer> balances = leaveService.getAllRemainingLeaves(employeeId);
            return ResponseEntity.ok(balances);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }


    // NEW ENDPOINT: Initialize leave balances from attendance service
    @PostMapping("/init-from-attendance")
    public ResponseEntity<String> initializeLeaveBalances() {
        leaveService.initializeAllEmployeesFromAttendance();
        return new ResponseEntity<>("Leave balances initialized from attendance service", HttpStatus.OK);
    }

    @GetMapping("/employee/{employeeId}/between")
    public ResponseEntity<List<Map<String, String>>> getLeavesBetweenDates(
            @PathVariable String employeeId,
            @RequestParam String from,
            @RequestParam String to) {

        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);

        List<EmployeeLeave> leaves = leaveService.getLeavesBetweenDates(employeeId, fromDate, toDate);

        // Convert to your desired JSON format
        List<Map<String, String>> response = leaves.stream().map(leave -> Map.of(
                "date", leave.getStartDate().toString(),  // or use each date if multi-day leave
                "leaveType", leave.getLeaveType().name(),
                "status", leave.getStatus().name()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

}
