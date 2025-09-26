package com.configserverllp.csllp_learning_platform.user_service.service.impl;
import com.configserverllp.csllp_learning_platform.user_service.dto.LoginRequest;
import com.configserverllp.csllp_learning_platform.user_service.dto.UserRequest;
import com.configserverllp.csllp_learning_platform.user_service.entity.User;
import com.configserverllp.csllp_learning_platform.user_service.exception.BadRequestException;
import com.configserverllp.csllp_learning_platform.user_service.exception.ResourceNotFoundException;
import com.configserverllp.csllp_learning_platform.user_service.repository.UserRepository;
import com.configserverllp.csllp_learning_platform.user_service.service.UserService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    @Override
    public User createUser(UserRequest req, Long creatorId, String creatorRole) {
        // check existing
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // validate managerId if present
        Optional.ofNullable(req.getManagerId()).ifPresent(mid -> {
            User m = userRepository.findById(mid).orElseThrow(() -> new BadRequestException("Manager not found"));
            if (!"MANAGER".equalsIgnoreCase(m.getRole())) throw new BadRequestException("Selected manager is not a manager");
        });

        // If creatorRole is MANAGER, ensure role to create is EMPLOYEE
        Optional.ofNullable(creatorRole)
                .filter(r -> r.equalsIgnoreCase("MANAGER"))
                .ifPresent(r -> {
                    if (!"EMPLOYEE".equalsIgnoreCase(req.getRole())) {
                        throw new BadRequestException("Manager can only create EMPLOYEE role");
                    }
                });

        User u = new User();
        u.setEmail(req.getEmail());
        u.setFirstName(req.getFirstName());
        u.setLastName(req.getLastName());
        u.setRole(req.getRole());
        u.setStatus("ACTIVE");
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());

        // If manager is creating, set managerId to creatorId
        Optional.ofNullable(creatorRole)
                .filter(r -> r.equalsIgnoreCase("MANAGER"))
                .ifPresent(r -> u.setManagerId(creatorId));

        // If admin provided managerId, set it (already validated earlier)
        Optional.ofNullable(req.getManagerId()).ifPresent(u::setManagerId);

        User saved = userRepository.save(u);
        sendCredentialsEmail(saved.getEmail(), req.getPassword());
        return saved;
    }

    private void sendCredentialsEmail(String to, String rawPassword) {
        try {
            Optional.ofNullable(mailSender).ifPresent(sender -> {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(to);
                msg.setSubject("Your account credentials - CSLLP");
                msg.setText("Your account has been created.\nUsername: " + to + "\nPassword: " + rawPassword + "\nPlease change password after first login.");
                sender.send(msg);
            });
        } catch (Exception ex) {
            // log & continue
            ex.printStackTrace();
        }
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public User updateUser(Long id, UserRequest update) {
        User existing = getUserById(id);
        existing.setFirstName(update.getFirstName());
        existing.setLastName(update.getLastName());
        existing.setRole(update.getRole());
        existing.setManagerId(update.getManagerId());
        existing.setUpdatedAt(LocalDateTime.now());

        // if password provided in update, you may choose to update. For safety, only update if non-null & non-empty:
        Optional.ofNullable(update.getPassword()).filter(p -> !p.trim().isEmpty()).ifPresent(p -> existing.setPassword(passwordEncoder.encode(p)));

        return userRepository.save(existing);
    }

    @Override
    public void softDeleteUser(Long id) {
        User u = getUserById(id);
        u.setStatus("INACTIVE");
        userRepository.save(u);
    }

    @Override
    public List<User> getUsersByManagerId(Long managerId) {
        return userRepository.findByManagerId(managerId);
    }

    @Override
    public List<User> getUsersByRole(String role) {
        if ("ALL".equalsIgnoreCase(role)) return userRepository.findAll();
        return userRepository.findByRole(role);
    }

    @Override
    public User login(LoginRequest req) {
        User u = userRepository.findByEmail(req.getEmail()).orElseThrow(() -> new BadRequestException("Invalid credentials"));
        if (!"ACTIVE".equalsIgnoreCase(u.getStatus())) throw new BadRequestException("User not active");
        if (!u.getRole().equalsIgnoreCase(req.getRole())) throw new BadRequestException("User does not have role: " + req.getRole());
        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) throw new BadRequestException("Invalid credentials");
        return u;
    }
}

