package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.UserResponse;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public UserService(UserRepository userRepository, CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public UserResponse me() {
        return UserResponse.from(currentUserService.getCurrentUser());
    }

    public List<UserResponse> listAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse updateRole(Long id, String role) {
        if (role == null || !(role.equals("ROLE_USER") || role.equals("ROLE_ADMIN"))) {
            throw ApiException.badRequest("El rol debe ser ROLE_USER o ROLE_ADMIN");
        }
        User target = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        User actor = currentUserService.getCurrentUser();
        if (actor.getId().equals(target.getId())) {
            throw ApiException.badRequest("No puedes cambiar tu propio rol");
        }
        target.setRole(role);
        return UserResponse.from(userRepository.save(target));
    }
}
