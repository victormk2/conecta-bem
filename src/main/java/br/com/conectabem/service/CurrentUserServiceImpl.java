package br.com.conectabem.service;

import br.com.conectabem.infra.CurrentUserContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserServiceImpl implements CurrentUserService {

    private final CurrentUserContext currentUserContext;

    public CurrentUserServiceImpl(CurrentUserContext currentUserContext) {
        this.currentUserContext = currentUserContext;
    }

    @Override
    public UUID requireUserId() {
        return currentUserContext.requireUserId();
    }

    @Override
    public String requireUsername() {
        return currentUserContext.requireUsername();
    }
}

