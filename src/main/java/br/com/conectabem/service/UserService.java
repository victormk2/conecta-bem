package br.com.conectabem.service;

import br.com.conectabem.dto.user.RegisterRequest;
import br.com.conectabem.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserService {
    User register(RegisterRequest request) throws IllegalArgumentException;
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
    Optional<User> authenticate(String email, String rawPassword);
}
