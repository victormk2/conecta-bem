package br.com.conectabem.service.impl;

import br.com.conectabem.infra.CurrentUserContext;
import br.com.conectabem.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserServiceImpl implements CurrentUserService {

    private final CurrentUserContext currentUserContext;

    @Override
    public UUID requireUserId() {
        return currentUserContext.requireUserId();
    }

    @Override
    public String requireUsername() {
        return currentUserContext.requireUsername();
    }
}

