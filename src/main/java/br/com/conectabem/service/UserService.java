package br.com.conectabem.service;

import br.com.conectabem.dto.user.UpdateProfileRequest;
import br.com.conectabem.model.User;

import java.util.UUID;

public interface UserService {

    User findById(UUID id);

    void updateProfile(UUID id, UpdateProfileRequest request);
}
