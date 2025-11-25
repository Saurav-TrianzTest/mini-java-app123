package com.test.repository;

import com.test.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for User entity
 * Provides automatic CRUD operations and custom query methods
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     * @param username the username to search for
     * @return Optional containing user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     * @param email the email to search for
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find all active users
     * @param active the active status
     * @return List of active users
     */
    List<User> findByActive(Boolean active);

    /**
     * Find users created after a specific date
     * @param createdAt the date to compare
     * @return List of users created after the date
     */
    List<User> findByCreatedAtAfter(LocalDateTime createdAt);

    /**
     * Find users by first name and last name
     * @param firstName the first name
     * @param lastName the last name
     * @return List of matching users
     */
    List<User> findByFirstNameAndLastName(String firstName, String lastName);

    /**
     * Custom query to find users with email containing a pattern
     * @param emailPattern the pattern to search for
     * @return List of matching users
     */
    @Query("SELECT u FROM User u WHERE u.email LIKE %:emailPattern%")
    List<User> findByEmailContaining(@Param("emailPattern") String emailPattern);

    /**
     * Count active users
     * @param active the active status
     * @return count of active users
     */
    long countByActive(Boolean active);

    /**
     * Check if username exists
     * @param username the username to check
     * @return true if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     * @param email the email to check
     * @return true if email exists
     */
    boolean existsByEmail(String email);
}
