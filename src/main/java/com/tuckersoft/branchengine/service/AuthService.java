package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.AuthResponse;
import com.tuckersoft.branchengine.dto.LoginRequest;
import com.tuckersoft.branchengine.dto.RegisterRequest;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.repository.UserRepository;
import com.tuckersoft.branchengine.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw ApiException.conflict("El email ya esta registrado");
        }
        User u = new User();
        u.setEmail(req.email());
        u.setPassword(passwordEncoder.encode(req.password()));  // BCrypt SIEMPRE
        u.setDisplayName(req.displayName());
        u.setRole("ROLE_USER");   // SIEMPRE ROLE_USER, aunque el JSON pida otra cosa
        u.setCreatedAt(Instant.now());
        userRepository.save(u);

        return AuthResponse.of(jwtService.generateToken(u.getEmail()),
                u.getEmail(), u.getDisplayName(), u.getRole());
    }

    public AuthResponse login(LoginRequest req) {
        // 401 siempre, sin revelar si fallo el email o la contrasena.
        User u = userRepository.findByEmail(req.email())
                .orElseThrow(() -> ApiException.unauthorized("Credenciales invalidas"));
        if (!passwordEncoder.matches(req.password(), u.getPassword())) {
            throw ApiException.unauthorized("Credenciales invalidas");
        }
        return AuthResponse.of(jwtService.generateToken(u.getEmail()),
                u.getEmail(), u.getDisplayName(), u.getRole());
    }
}
