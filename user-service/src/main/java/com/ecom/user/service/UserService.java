package com.ecom.user.service;

import com.ecom.common.dto.ApiResponse;
import com.ecom.common.event.BaseEvent;
import com.ecom.common.event.EventTypes;
import com.ecom.common.event.TopicNames;
import com.ecom.common.exception.DuplicateResourceException;
import com.ecom.common.exception.ResourceNotFoundException;
import com.ecom.user.dto.CreateUserRequest;
import com.ecom.user.dto.UserResponse;
import com.ecom.user.entity.User;
import com.ecom.user.repository.UserRepository;
import io.awspring.cloud.sns.core.SnsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final SnsTemplate snsTemplate;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User already exists with email: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(hashPassword(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .build();

        user = userRepository.save(user);
        log.info("User created: id={}, email={}", user.getId(), user.getEmail());

        // Publish USER_REGISTERED event to SNS
        publishUserRegisteredEvent(user);

        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateUser(String id, CreateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user = userRepository.save(user);

        log.info("User updated: id={}", user.getId());
        return toResponse(user);
    }

    // ── Event Publishing ──

    private void publishUserRegisteredEvent(User user) {
        try {
            BaseEvent event = BaseEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(EventTypes.USER_REGISTERED)
                    .source("user-service")
                    .timestamp(Instant.now())
                    .correlationId(UUID.randomUUID().toString())
                    .data(Map.of(
                            "userId", user.getId(),
                            "email", user.getEmail(),
                            "fullName", user.getFullName()))
                    .build();

            snsTemplate.convertAndSend(TopicNames.USER_EVENTS, event);
            log.info("Published USER_REGISTERED event for userId={}", user.getId());
        } catch (Exception e) {
            // Don't fail user creation if event publishing fails
            log.error("Failed to publish USER_REGISTERED event for userId={}", user.getId(), e);
        }
    }

    // ── Helpers ──

    private String hashPassword(String password) {
        // TODO: Replace with BCrypt in production
        return "{bcrypt}" + password.hashCode();
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
