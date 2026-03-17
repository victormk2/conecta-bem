package br.com.conectabem.infra;

import br.com.conectabem.model.User;
import br.com.conectabem.model.UserRole;
import br.com.conectabem.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserInterceptorTest {

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private CurrentUserInterceptor interceptor;

    @BeforeEach
    void setup() {
        interceptor = new CurrentUserInterceptor(currentUserContext, userRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preHandleDoesNothingWhenNoAuthentication() {
        interceptor.preHandle(request, response, new Object());

        verify(userRepository, never()).findByUsername(org.mockito.Mockito.anyString());
        verify(currentUserContext, never()).set(org.mockito.Mockito.any(), org.mockito.Mockito.anyString());
    }

    @Test
    void preHandleLoadsUserByUsernameAndSetsContext() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("user");
        user.setEmail("u@e.com");
        user.setFullName("User");
        user.setRole(UserRole.USER);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null)
        );
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

        interceptor.preHandle(request, response, new Object());

        verify(currentUserContext).set(userId, "user");
    }

    @Test
    void afterCompletionAlwaysClearsContext() {
        interceptor.afterCompletion(request, response, new Object(), null);
        verify(currentUserContext).clear();
    }
}

