package br.com.conectabem.service.auth.factory;

import br.com.conectabem.dto.user.RegisterRequest;
import br.com.conectabem.model.User;
import br.com.conectabem.model.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DefaultUserFactory implements UserFactory {

    private final PasswordEncoder passwordEncoder;

    public DefaultUserFactory(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User create(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setRole(UserRole.USER);
        user.setPassword(passwordEncoder.encode(request.password()));
        return user;
    }
}

