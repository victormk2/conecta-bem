package br.com.conectabem.infra;

import br.com.conectabem.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CurrentUserInterceptor implements HandlerInterceptor {

    private final CurrentUserContext currentUserContext;
    private final UserRepository userRepository;

    public CurrentUserInterceptor(CurrentUserContext currentUserContext, UserRepository userRepository) {
        this.currentUserContext = currentUserContext;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return true;
        }

        String username = authentication.getPrincipal().toString();
        userRepository.findByUsername(username).ifPresent(user -> currentUserContext.set(user.getId(), user.getUsername()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        currentUserContext.clear();
    }
}

