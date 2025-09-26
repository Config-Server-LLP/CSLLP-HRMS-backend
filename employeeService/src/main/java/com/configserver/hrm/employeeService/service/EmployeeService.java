package com.configserver.hrm.employeeService.service;

import com.configserver.hrm.employeeService.dto.EmployeeDTO;
import com.configserver.hrm.employeeService.entity.Employee;

import java.util.List;

public interface EmployeeService {
    void registerEmployee(EmployeeDTO employeeDTO);
    List<Employee> getAllEmployees();
    Employee getEmployeeById(Long id);
    void updateEmployee(Long id, EmployeeDTO employeeDTO);
    void deleteEmployee(Long id);
    void registerEmployeesFromAttendance();
}
