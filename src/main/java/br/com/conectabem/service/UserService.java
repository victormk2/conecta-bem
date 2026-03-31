package br.com.conectabem.service;

import br.com.conectabem.model.User;

import java.util.UUID;

public interface UserService {

    User findById(UUID id);
}
