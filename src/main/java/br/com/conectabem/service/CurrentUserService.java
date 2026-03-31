package br.com.conectabem.service;

import java.util.UUID;

public interface CurrentUserService {
    UUID requireUserId();
    String requireUsername();
}

