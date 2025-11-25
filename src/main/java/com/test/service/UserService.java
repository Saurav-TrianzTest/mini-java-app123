package com.test.service;

import com.test.entity.User;
import com.test.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User Service with proper transaction management
 * Demonstrates Spring's declarative transaction management
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * Find user by ID
     * @param id user ID
     * @return Optional containing user if found
     */
    public Optional<User> findById(Long id) {
        log.debug("Finding user by id: {}", id);
        return userRepository.findById(id);
    }

    /**
     * Find user by username
     * @param username the username
     * @return Optional containing user if found
     */
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsername(username);
    }

    /**
     * Find user by email
     * @param email the email
     * @return Optional containing user if found
     */
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Find all active users
     * @return List of active users
     */
    public List<User> findAllActiveUsers() {
        log.debug("Finding all active users");
        return userRepository.findByActive(true);
    }

    /**
     * Find all users
     * @return List of all users
     */
    public List<User> findAll() {
        log.debug("Finding all users");
        return userRepository.findAll();
    }

    /**
     * Create a new user
     * @param user the user to create
     * @return created user
     */
    @Transactional(
        propagation = Propagation.REQUIRED,
        isolation = Isolation.READ_COMMITTED,
        readOnly = false
    )
    public User createUser(User user) {
        log.info("Creating new user: {}", user.getUsername());

        // Validation
        if (userRepository.existsByUsername(user.getUsername())) {
            log.error("Username already exists: {}", user.getUsername());
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            log.error("Email already exists: {}", user.getEmail());
            throw new IllegalArgumentException("Email already exists");
        }

        User savedUser = userRepository.save(user);
        log.info("User created successfully with id: {}", savedUser.getId());
        return savedUser;
    }

    /**
     * Update an existing user
     * @param id user ID
     * @param updatedUser updated user data
     * @return updated user
     */
    @Transactional(
        propagation = Propagation.REQUIRED,
        isolation = Isolation.READ_COMMITTED,
        readOnly = false
    )
    public User updateUser(Long id, User updatedUser) {
        log.info("Updating user with id: {}", id);

        User existingUser = userRepository.findById(id)
            .orElseThrow(() -> {
                log.error("User not found with id: {}", id);
                return new IllegalArgumentException("User not found");
            });

        // Update fields
        if (updatedUser.getUsername() != null) {
            existingUser.setUsername(updatedUser.getUsername());
        }
        if (updatedUser.getEmail() != null) {
            existingUser.setEmail(updatedUser.getEmail());
        }
        if (updatedUser.getFirstName() != null) {
            existingUser.setFirstName(updatedUser.getFirstName());
        }
        if (updatedUser.getLastName() != null) {
            existingUser.setLastName(updatedUser.getLastName());
        }
        if (updatedUser.getActive() != null) {
            existingUser.setActive(updatedUser.getActive());
        }

        User saved = userRepository.save(existingUser);
        log.info("User updated successfully: {}", saved.getId());
        return saved;
    }

    /**
     * Delete user by ID
     * @param id user ID
     */
    @Transactional(
        propagation = Propagation.REQUIRED,
        isolation = Isolation.READ_COMMITTED,
        readOnly = false
    )
    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);

        if (!userRepository.existsById(id)) {
            log.error("User not found with id: {}", id);
            throw new IllegalArgumentException("User not found");
        }

        userRepository.deleteById(id);
        log.info("User deleted successfully: {}", id);
    }

    /**
     * Deactivate user
     * @param id user ID
     */
    @Transactional(
        propagation = Propagation.REQUIRED,
        isolation = Isolation.READ_COMMITTED,
        readOnly = false
    )
    public void deactivateUser(Long id) {
        log.info("Deactivating user with id: {}", id);

        User user = userRepository.findById(id)
            .orElseThrow(() -> {
                log.error("User not found with id: {}", id);
                return new IllegalArgumentException("User not found");
            });

        user.setActive(false);
        userRepository.save(user);
        log.info("User deactivated successfully: {}", id);
    }

    /**
     * Count total users
     * @return total user count
     */
    public long countUsers() {
        return userRepository.count();
    }

    /**
     * Count active users
     * @return active user count
     */
    public long countActiveUsers() {
        return userRepository.countByActive(true);
    }

    /**
     * Find recently created users
     * @param days number of days to look back
     * @return List of recently created users
     */
    public List<User> findRecentlyCreatedUsers(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        log.debug("Finding users created since: {}", since);
        return userRepository.findByCreatedAtAfter(since);
    }
}
