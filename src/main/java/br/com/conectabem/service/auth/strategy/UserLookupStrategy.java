package br.com.conectabem.service.auth.strategy;

import br.com.conectabem.model.User;

import java.util.Optional;

public interface UserLookupStrategy {
    boolean supports(String identifier);

    Optional<User> find(String identifier);
}

