package br.com.conectabem.service;

import br.com.conectabem.dto.user.RegisterRequest;
import br.com.conectabem.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for user management operations.
 *
 * Provides methods for user registration, authentication, and retrieval.
 * Implementations should handle password hashing, validation, and database operations.
 *
 * @author conecta-bem
 * @version 1.0
 */
public interface UserService {

    /**
     * Registers a new user with the provided registration data.
     *
     * @param request the registration request containing user details (email, password, name)
     * @return the created User entity with hashed password
     * @throws IllegalArgumentException if email already exists or validation fails
     */
    User register(RegisterRequest request) throws IllegalArgumentException;

    /**
     * Finds a user by their email address.
     *
     * @param email the user's email address
     * @return an Optional containing the User if found, empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by their unique identifier.
     *
     * @param id the user's UUID
     * @return an Optional containing the User if found, empty otherwise
     */
    Optional<User> findById(UUID id);

    /**
     * Authenticates a user with email and password.
     *
     * @param email the user's email address
     * @param rawPassword the plaintext password to verify
     * @return an Optional containing the User if authentication succeeds, empty otherwise
     */
    Optional<User> authenticate(String email, String rawPassword);
}
