package com.configserverllp.csllp_learning_platform.user_service.controller;
import com.configserverllp.csllp_learning_platform.user_service.dto.LoginRequest;
import com.configserverllp.csllp_learning_platform.user_service.dto.LoginResponse;
import com.configserverllp.csllp_learning_platform.user_service.dto.UserRequest;
import com.configserverllp.csllp_learning_platform.user_service.dto.UserResponse;
import com.configserverllp.csllp_learning_platform.user_service.entity.User;
import com.configserverllp.csllp_learning_platform.user_service.service.UserService;
import com.configserverllp.csllp_learning_platform.user_service.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody UserRequest req,
            @RequestHeader(value = "X-Creator-Id", required = false) Long creatorId,
            @RequestHeader(value = "X-Creator-Role", required = false) String creatorRole) {

        String effectiveCreatorRole = creatorRole == null ? "ADMIN" : creatorRole;
        User saved = userService.createUser(req, creatorId, effectiveCreatorRole);
        UserResponse resp = mapToResponse(saved);
        return ResponseEntity.status(201).body(new ApiResponse<>(true, "User created", resp));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest req) {
        User u = userService.login(req);
        LoginResponse resp = new LoginResponse(u.getId(), u.getEmail(), u.getRole(), u.getManagerId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Login successful", resp));
    }

    private UserResponse mapToResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .role(u.getRole())
                .managerId(u.getManagerId())
                .status(u.getStatus())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}
