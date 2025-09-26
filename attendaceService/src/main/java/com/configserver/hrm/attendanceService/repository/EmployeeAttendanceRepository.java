package com.configserver.hrm.attendanceService.repository;

import com.configserver.hrm.attendanceService.entity.EmployeeAttendance;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmployeeAttendanceRepository extends JpaRepository<EmployeeAttendance, String> {
    List<EmployeeAttendance> findByDate(LocalDate date);
    List<EmployeeAttendance> findByEmployeeIdAndDateBetween(String employeeId, LocalDate from, LocalDate to);
    boolean existsByEmployeeIdAndDate(String employeeId, LocalDate date);
    @Transactional

    void deleteByDate(LocalDate date);

}

