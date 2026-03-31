package br.com.conectabem.infra;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUserContext {

    private static final ThreadLocal<UUID> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    public void set(UUID userId, String username) {
        USER_ID.set(userId);
        USERNAME.set(username);
    }

    public UUID requireUserId() {
        UUID id = USER_ID.get();
        if (id == null) {
            throw new IllegalArgumentException("authentication required");
        }
        return id;
    }

    public String requireUsername() {
        String username = USERNAME.get();
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("authentication required");
        }
        return username;
    }

    public void clear() {
        USER_ID.remove();
        USERNAME.remove();
    }
}

