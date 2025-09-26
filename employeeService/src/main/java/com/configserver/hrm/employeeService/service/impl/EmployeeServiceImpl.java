package com.configserver.hrm.employeeService.service.impl;

import com.configserver.hrm.employeeService.client.AttendanceClient;
import com.configserver.hrm.employeeService.dto.EmployeeDTO;
import com.configserver.hrm.employeeService.entity.Employee;
import com.configserver.hrm.employeeService.entity.Role;
import com.configserver.hrm.employeeService.exception.EmployeeException;
import com.configserver.hrm.employeeService.mail.EmailService;
import com.configserver.hrm.employeeService.repository.EmployeeRepository;
import com.configserver.hrm.employeeService.service.EmployeeService;
import com.configserver.hrm.employeeService.util.PasswordGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AttendanceClient attendanceClient;

    @Override
    public void registerEmployee(EmployeeDTO employeeDTO) {
        repository.findByEmail(employeeDTO.getEmail())
                .ifPresent(emp -> { throw new EmployeeException("Email already registered!"); });

        boolean isFirstAdmin = repository.count() == 0 && employeeDTO.getRole() == Role.ADMIN;
        if (!isFirstAdmin && employeeDTO.getRole() == Role.ADMIN) {
            throw new EmployeeException("Only an existing admin can register another admin!");
        }

        String rawPassword = PasswordGenerator.generatePassword();
        String hashedPassword = passwordEncoder.encode(rawPassword);

        Employee employee = new Employee();
        employee.setName(employeeDTO.getName());
        employee.setEmail(employeeDTO.getEmail());
        employee.setRole(employeeDTO.getRole());
        employee.setPassword(hashedPassword);

        repository.save(employee);
        emailService.sendPasswordEmail(employeeDTO.getEmail(), rawPassword);
    }

    @Override
    public List<Employee> getAllEmployees() {
        return repository.findAll();
    }

    @Override
    public Employee getEmployeeById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EmployeeException("Employee not found with id: " + id));
    }

    @Override
    public void updateEmployee(Long id, EmployeeDTO employeeDTO) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EmployeeException("Employee not found with id: " + id));

        employee.setName(employeeDTO.getName());
        employee.setEmail(employeeDTO.getEmail());
        employee.setRole(employeeDTO.getRole());

        repository.save(employee);
    }

    @Override
    public void deleteEmployee(Long id) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EmployeeException("Employee not found with id: " + id));
        repository.delete(employee);
    }

    @Override
    public void registerEmployeesFromAttendance() {
        List<Map<String, Object>> attendanceEmployees = attendanceClient.fetchEmployeesFromAttendance();

        if (attendanceEmployees == null || attendanceEmployees.isEmpty()) {
            System.out.println("No employees fetched from attendance service");
            return;
        }

        for (Map<String, Object> empData : attendanceEmployees) {
            if (empData == null) continue;  // skip null entries

            String email = (String) empData.get("email");
            String name = (String) empData.get("name");

            if (email == null || email.isEmpty()) continue; // skip invalid emails

            if (repository.findByEmail(email).isEmpty()) {
                EmployeeDTO dto = new EmployeeDTO();
                dto.setName(name != null ? name : "Unknown");
                dto.setEmail(email);
                dto.setRole(Role.EMPLOYEE);
                registerEmployee(dto);
            }
        }
    }

}
