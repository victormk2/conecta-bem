package br.com.conectabem.infra;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrentUserContextTest {

    private final CurrentUserContext ctx = new CurrentUserContext();

    @AfterEach
    void tearDown() {
        ctx.clear();
    }

    @Test
    void requireMethodsThrowWhenNotAuthenticated() {
        assertThrows(IllegalArgumentException.class, ctx::requireUserId);
        assertThrows(IllegalArgumentException.class, ctx::requireUsername);
    }

    @Test
    void setThenRequireThenClearWorks() {
        UUID userId = UUID.randomUUID();
        ctx.set(userId, "user");

        assertEquals(userId, ctx.requireUserId());
        assertEquals("user", ctx.requireUsername());

        ctx.clear();
        assertThrows(IllegalArgumentException.class, ctx::requireUserId);
        assertThrows(IllegalArgumentException.class, ctx::requireUsername);
    }
}

