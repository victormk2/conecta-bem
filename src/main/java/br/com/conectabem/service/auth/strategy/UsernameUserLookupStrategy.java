package br.com.conectabem.service.auth.strategy;

import br.com.conectabem.model.User;
import br.com.conectabem.repository.UserRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(2)
public class UsernameUserLookupStrategy implements UserLookupStrategy {

    private final UserRepository repository;

    public UsernameUserLookupStrategy(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean supports(String identifier) {
        return identifier != null && !identifier.contains("@");
    }

    @Override
    public Optional<User> find(String identifier) {
        return repository.findByUsername(identifier);
    }
}

