package com.api.service.internal;

import com.api.dto.common.PageRequest;
import com.api.dto.common.PageResponse;
import com.api.dto.user.CreateUserRequest;
import com.api.dto.user.UpdateUserRequest;
import com.api.dto.user.UserResponse;
import com.api.exception.ConflictException;
import com.api.exception.OptimisticLockException;
import com.api.exception.ResourceNotFoundException;
import com.api.model.User;
import com.api.repository.UserRepository;
import com.api.util.PaginationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Internal service for user business logic.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PaginationHelper paginationHelper;

    public UserService(UserRepository userRepository, PaginationHelper paginationHelper) {
        this.userRepository = userRepository;
        this.paginationHelper = paginationHelper;
    }

    /**
     * Create a new user.
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.getEmail());

        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("User with email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(request.getRole())
                .status("ACTIVE")
                .build();

        User savedUser = userRepository.create(user);
        log.info("User created with ID: {}", savedUser.getId());

        return UserResponse.fromEntity(savedUser);
    }

    /**
     * Get user by ID.
     */
    @Cacheable(value = "users", key = "#id")
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.debug("Fetching user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        return UserResponse.fromEntity(user);
    }

    /**
     * Get user by email.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.debug("Fetching user with email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return UserResponse.fromEntity(user);
    }

    /**
     * Get all users with pagination.
     */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(PageRequest pageRequest) {
        log.debug("Fetching all users with pagination: {}", pageRequest);

        PageRequest normalized = paginationHelper.normalize(pageRequest);
        List<User> users = userRepository.findAll(normalized);
        long totalCount = userRepository.count();

        List<UserResponse> responses = users.stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());

        return paginationHelper.buildPageResponse(responses, totalCount, normalized);
    }

    /**
     * Get users by status with pagination.
     */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsersByStatus(String status, PageRequest pageRequest) {
        log.debug("Fetching users with status: {} and pagination: {}", status, pageRequest);

        PageRequest normalized = paginationHelper.normalize(pageRequest);
        List<User> users = userRepository.findByStatus(status, normalized);
        long totalCount = userRepository.countByStatus(status);

        List<UserResponse> responses = users.stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());

        return paginationHelper.buildPageResponse(responses, totalCount, normalized);
    }

    /**
     * Search users by name.
     */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(String searchTerm, PageRequest pageRequest) {
        log.debug("Searching users with term: {} and pagination: {}", searchTerm, pageRequest);

        PageRequest normalized = paginationHelper.normalize(pageRequest);
        List<User> users = userRepository.searchByName(searchTerm, normalized);

        List<UserResponse> responses = users.stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());

        // Note: For proper pagination, you'd need a separate count query
        return paginationHelper.buildPageResponse(responses, responses.size(), normalized);
    }

    /**
     * Update an existing user.
     */
    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        log.info("Updating user with ID: {}", id);

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        // Check for email conflict if email is being changed
        if (request.getEmail() != null && !request.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("User with email already exists: " + request.getEmail());
            }
            existingUser.setEmail(request.getEmail());
        }

        // Update fields
        if (request.getFirstName() != null) {
            existingUser.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            existingUser.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            existingUser.setPhone(request.getPhone());
        }
        if (request.getStatus() != null) {
            existingUser.setStatus(request.getStatus());
        }
        if (request.getRole() != null) {
            existingUser.setRole(request.getRole());
        }

        existingUser.setVersion(request.getVersion());

        User updatedUser = userRepository.update(existingUser)
                .orElseThrow(() -> new OptimisticLockException(
                        "User was modified by another transaction. Please refresh and try again."));

        log.info("User updated successfully with ID: {}", id);
        return UserResponse.fromEntity(updatedUser);
    }

    /**
     * Soft delete a user.
     */
    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with ID: " + id);
        }

        boolean deleted = userRepository.deleteById(id);
        if (!deleted) {
            throw new ResourceNotFoundException("User not found with ID: " + id);
        }

        log.info("User deleted successfully with ID: {}", id);
    }
}
