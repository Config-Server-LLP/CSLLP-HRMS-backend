package com.configserver.hrm.employeeService.controller;

import com.configserver.hrm.employeeService.entity.User;
import com.configserver.hrm.employeeService.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    // HR/Admin/Manager self-register
    @PostMapping("/register-admin")
    public ResponseEntity<User> registerAdminOrManager(@RequestBody User user) {
        User savedUser = service.createAdminOrManager(user);
        savedUser.setPassword(null);
        return ResponseEntity.ok(savedUser);
    }

    // Create Employee by HR/Admin/Manager
    @PostMapping("/create-employee")
    public ResponseEntity<User> createEmployee(@RequestBody User employee,
                                               @RequestHeader("createdByRole") String role) {
        User savedEmployee = service.createEmployee(employee, role);
        savedEmployee.setPassword(null);
        return ResponseEntity.ok(savedEmployee);
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String email, @RequestParam String password) {
        boolean success = service.login(email, password);
        if (success) {
            return ResponseEntity.ok("Login successful");
        } else {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }
}
