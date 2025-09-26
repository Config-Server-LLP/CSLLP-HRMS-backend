package com.configserverllp.csllp_learning_platform.user_service.controller;

import com.configserverllp.csllp_learning_platform.user_service.dto.UserRequest;
import com.configserverllp.csllp_learning_platform.user_service.dto.UserResponse;
import com.configserverllp.csllp_learning_platform.user_service.entity.User;
import com.configserverllp.csllp_learning_platform.user_service.service.UserService;
import com.configserverllp.csllp_learning_platform.user_service.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        User u = userService.getUserById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "User fetched", mapToResponse(u)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest updateReq) {
        User updated = userService.updateUser(id, updateReq);
        return ResponseEntity.ok(new ApiResponse<>(true, "User updated", mapToResponse(updated)));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.softDeleteUser(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "User soft-deleted", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers(@RequestParam(value = "managerId", required = false) Long managerId) {
        List<User> users = (managerId == null) ? userService.getUsersByRole("ALL") : userService.getUsersByManagerId(managerId);
        List<UserResponse> resp = users.stream().map(this::mapToResponse).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Users fetched", resp));
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(@PathVariable String role) {
        List<User> users = userService.getUsersByRole(role);
        List<UserResponse> resp = users.stream().map(this::mapToResponse).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Users fetched", resp));
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
