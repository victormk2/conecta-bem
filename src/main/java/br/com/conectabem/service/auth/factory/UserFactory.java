package br.com.conectabem.service.auth.factory;

import br.com.conectabem.dto.user.RegisterRequest;
import br.com.conectabem.model.User;

public interface UserFactory {
    User create(RegisterRequest request);
}

