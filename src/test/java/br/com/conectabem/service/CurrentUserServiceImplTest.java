package br.com.conectabem.service;

import br.com.conectabem.infra.CurrentUserContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserServiceImplTest {

    @Test
    void delegatesToContext() {
        CurrentUserContext context = mock(CurrentUserContext.class);
        CurrentUserServiceImpl service = new CurrentUserServiceImpl(context);

        UUID userId = UUID.randomUUID();
        when(context.requireUserId()).thenReturn(userId);
        when(context.requireUsername()).thenReturn("user");

        assertEquals(userId, service.requireUserId());
        assertEquals("user", service.requireUsername());
    }
}

