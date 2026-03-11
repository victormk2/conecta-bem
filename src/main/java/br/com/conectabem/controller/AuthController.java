package br.com.conectabem.controller;

import br.com.conectabem.dto.user.LoginRequest;
import br.com.conectabem.dto.user.LoginResponse;
import br.com.conectabem.dto.user.RegisterRequest;
import br.com.conectabem.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public LoginResponse register(@RequestBody RegisterRequest request) {
        return new LoginResponse(service.register(request), Instant.now().plusSeconds(86400));
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return new LoginResponse(service.login(request), Instant.now().plusSeconds(86400));
    }
}