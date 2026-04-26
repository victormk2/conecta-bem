package br.com.conectabem.service.impl;

import br.com.conectabem.dto.user.UpdateProfileRequest;
import br.com.conectabem.model.User;
import br.com.conectabem.repository.UserRepository;
import br.com.conectabem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

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

        userRepository.save(user);
    }
}
