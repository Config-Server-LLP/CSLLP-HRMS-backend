package com.configserverllp.csllp_learning_platform.user_service.service;
import com.configserverllp.csllp_learning_platform.user_service.dto.LoginRequest;
import com.configserverllp.csllp_learning_platform.user_service.dto.UserRequest;
import com.configserverllp.csllp_learning_platform.user_service.entity.User;

import java.util.List;

public interface UserService {
    User createUser(UserRequest req, Long creatorId, String creatorRole);
    User getUserById(Long id);
    User updateUser(Long id, UserRequest update);
    void softDeleteUser(Long id);
    List<User> getUsersByManagerId(Long managerId);
    List<User> getUsersByRole(String role);
    User login(LoginRequest req);
}
