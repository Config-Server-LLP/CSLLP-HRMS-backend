package com.configserver.hrm.attendanceService.controller;

import com.configserver.hrm.attendanceService.dto.AttendanceRequestDTO;
import com.configserver.hrm.attendanceService.entity.EmployeeAttendance;
import com.configserver.hrm.attendanceService.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    // ✅ Manual import (POST JSON body with validation)
    @PostMapping("/import")
    public ResponseEntity<String> importAttendance(@RequestBody @Valid List<AttendanceRequestDTO> attendanceList) {
        if (attendanceList == null || attendanceList.isEmpty()) {
            return ResponseEntity.badRequest().body("Attendance list cannot be empty");
        }
        attendanceService.importAttendance(attendanceList);
        return ResponseEntity.ok("Attendance Imported Successfully");
    }

    // ✅ Daily import (today) - return Entity instead of DTO
    @PostMapping("/import/daily")
    public ResponseEntity<List<EmployeeAttendance>> importTodayAttendance() {
        List<EmployeeAttendance> importedData = attendanceService.importDailyAttendanceFromEtimeOffice();
        return ResponseEntity.ok(importedData.isEmpty() ? Collections.emptyList() : importedData);
    }

    // ✅ Import by custom date - return Entity instead of DTO
    @PostMapping("/import/by-date")
    public ResponseEntity<List<EmployeeAttendance>> importAttendanceByDate(@RequestParam String date) {
        LocalDate reportDate;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            reportDate = LocalDate.parse(date, formatter);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }

        List<EmployeeAttendance> importedData = attendanceService.importAttendanceFromEtimeOffice(reportDate);
        return ResponseEntity.ok(importedData.isEmpty() ? Collections.emptyList() : importedData);
    }

   // ✅ Download Attendance Report API
   @GetMapping("/download")
   public ResponseEntity<ByteArrayResource> downloadAttendanceReport(@RequestParam String date) {
       LocalDate reportDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy"));

       byte[] reportData = attendanceService.downloadAttendanceReport(reportDate);

       ByteArrayResource resource = new ByteArrayResource(reportData);

       HttpHeaders headers = new HttpHeaders();
       headers.setContentType(MediaType.parseMediaType(
               "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
       headers.setContentDisposition(ContentDisposition.attachment()
               .filename("Attendance_Report_" + reportDate + ".xlsx")
               .build());

       return ResponseEntity.ok()
               .headers(headers)
               .body(resource);
   }
    @GetMapping("/employees-info")
    public ResponseEntity<List<Map<String, Object>>> getEmployeesInfo() {
        List<Map<String, Object>> employees = attendanceService.getEmployeesFromAttendance();
        return ResponseEntity.ok(employees);
    }

}
