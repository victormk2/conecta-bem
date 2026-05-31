package br.com.conectabem.service.impl;

import br.com.conectabem.dto.user.UpdatePasswordRequest;
import br.com.conectabem.dto.user.UpdateProfileRequest;
import br.com.conectabem.model.User;
import br.com.conectabem.repository.UserRepository;
import br.com.conectabem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User findById(UUID id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public void updateProfile(UUID id, UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.email()     != null) user.setEmail(request.email());
        if (request.gender()    != null) user.setGender(request.gender());
        if (request.phone()     != null) user.setPhone(request.phone());
        if (request.instagram() != null) user.setInstagram(request.instagram());
        if (request.linkedin()  != null) user.setLinkedin(request.linkedin());
        if (request.cpfCnpj()   != null) user.setCpfCnpj(request.cpfCnpj());

        userRepository.save(user);
    }

    @Override
    public void updatePassword(UUID id, UpdatePasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.getEmail().equals(request.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email mismatch");
        }
        if (request.newPassword() == null || request.newPassword().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password is required");
        }

        boolean usingTempPassword = request.temporaryPassword() != null;

        if (usingTempPassword) {
            if (user.getTemporaryPassword() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No temporary password set");
            }
            if (Instant.now().isAfter(user.getTemporaryPasswordExpiresAt())) {
                user.setTemporaryPassword(null);
                user.setTemporaryPasswordExpiresAt(null);
                userRepository.save(user);
                throw new ResponseStatusException(HttpStatus.GONE, "Temporary password has expired");
            }
            if (!passwordEncoder.matches(request.temporaryPassword(), user.getTemporaryPassword())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid temporary password");
            }
        } else {
            if (request.currentPassword() == null || request.currentPassword().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is required");
            }
            if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
            }
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setTemporaryPassword(null);
        user.setTemporaryPasswordExpiresAt(null);
        userRepository.save(user);
    }
}
