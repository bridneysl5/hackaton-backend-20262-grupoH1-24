package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) { this.userRepository = userRepository; }

    // El "usuario autenticado actual": email del SecurityContext + busqueda en BD.
    public User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw ApiException.unauthorized("No hay usuario autenticado");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> ApiException.unauthorized("No hay usuario autenticado"));
    }

    public boolean isAdmin(User user) {
        return "ROLE_ADMIN".equals(user.getRole());
    }
}
