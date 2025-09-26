package com.configserver.hrm.employeeService.controller;

import com.configserver.hrm.employeeService.dto.EmployeeDTO;
import com.configserver.hrm.employeeService.entity.Employee;
import com.configserver.hrm.employeeService.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    // Existing register API
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody EmployeeDTO dto) {
        employeeService.registerEmployee(dto);
        return ResponseEntity.ok("Employee registered and password sent to email!");
    }

    // Get all employees
    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    // Get employee by ID
    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    // Update employee
    @PutMapping("/{id}")
    public ResponseEntity<String> updateEmployee(@PathVariable Long id, @RequestBody EmployeeDTO dto) {
        employeeService.updateEmployee(id, dto);
        return ResponseEntity.ok("Employee updated successfully!");
    }

    // Delete employee
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok("Employee deleted successfully!");
    }

    // ✅ New API: Fetch employees from Attendance and register
    @PostMapping("/import-from-attendance")
    public ResponseEntity<String> importEmployeesFromAttendance() {
        employeeService.registerEmployeesFromAttendance();
        return ResponseEntity.ok("Employees imported from Attendance successfully!");
    }
}
