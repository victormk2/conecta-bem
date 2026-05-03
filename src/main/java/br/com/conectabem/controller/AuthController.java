package br.com.conectabem.controller;

import br.com.conectabem.dto.user.ForgotPasswordRequest;
import br.com.conectabem.dto.user.LoginRequest;
import br.com.conectabem.dto.user.LoginResponse;
import br.com.conectabem.dto.user.RegisterRequest;
import br.com.conectabem.service.AuthService;
import br.com.conectabem.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public LoginResponse register(@RequestBody RegisterRequest request) {
        var token = service.register(request);
        var id = service.getUserId(request.username());
        return new LoginResponse(token, Instant.now().plusSeconds(86400), id);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        var token = service.login(request);
        var id = service.getUserId(request.username());
        return new LoginResponse(token, Instant.now().plusSeconds(86400), id);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordResetService.forgotPassword(request.email());
        return ResponseEntity.ok().build();
    }
}