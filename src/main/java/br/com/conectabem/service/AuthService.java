package br.com.conectabem.service;

import br.com.conectabem.dto.user.LoginRequest;
import br.com.conectabem.dto.user.RegisterRequest;
import br.com.conectabem.model.User;
import br.com.conectabem.model.UserRole;
import br.com.conectabem.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository repository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {

        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public String register(RegisterRequest request) {

        User user = new User();

        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setRole(UserRole.USER);

        user.setPassword(passwordEncoder.encode(request.password()));

        repository.save(user);

        return jwtService.generateToken(user.getUsername());
    }

    public String login(LoginRequest request) {

        User user = repository.findByUsername(request.username())
                .orElseThrow();

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return jwtService.generateToken(user.getUsername());
    }

    public boolean checkAccess(String token) {
        try {
            String username = jwtService.extractUsername(token);
            return repository.findByUsername(username).isPresent();
        } catch (Exception e) {
            return false;
        }
    }
}
