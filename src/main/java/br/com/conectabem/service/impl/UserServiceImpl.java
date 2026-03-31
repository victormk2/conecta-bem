package br.com.conectabem.service.impl;

import br.com.conectabem.model.User;
import br.com.conectabem.repository.UserRepository;
import br.com.conectabem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User findById(UUID id) {
        return userRepository.findById(id).orElse(null);
    }
}
