package com.configserver.hrm.attendanceService.service;

import com.configserver.hrm.attendanceService.dto.AttendanceRequestDTO;
import com.configserver.hrm.attendanceService.entity.EmployeeAttendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AttendanceService {

    void importAttendance(List<AttendanceRequestDTO> attendanceList);

    // ✅ Today's daily attendance (returns saved entity objects)
    List<EmployeeAttendance> importDailyAttendanceFromEtimeOffice();

    // ✅ Custom date attendance (returns saved entity objects)
    List<EmployeeAttendance> importAttendanceFromEtimeOffice(LocalDate date);

    // ✅ Get all attendance for a date
    List<EmployeeAttendance> getDailyAttendance(LocalDate date);

    // ✅ Get employee attendance between dates
    List<EmployeeAttendance> getEmployeeAttendance(String employeeId, LocalDate from, LocalDate to);

    // ✅ Download attendance report (Excel bytes)
    byte[] downloadAttendanceReport(LocalDate date);

    List<Map<String, Object>> getEmployeesFromAttendance();
}
