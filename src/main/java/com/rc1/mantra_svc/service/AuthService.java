package com.rc1.mantra_svc.service;

import com.rc1.mantra_svc.dto.auth.AuthRequest;
import com.rc1.mantra_svc.dto.auth.AuthResponse;
import com.rc1.mantra_svc.dto.auth.RegisterRequest;
import com.rc1.mantra_svc.model.User;
import com.rc1.mantra_svc.repository.UserRepository;
import com.rc1.mantra_svc.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Handles user registration, login, and identity lookup.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final String[] AVATAR_COLORS = {
            "#6C63FF", "#FF6584", "#43CBFF", "#F9748F",
            "#00D9FF", "#FF9800", "#4CAF50", "#9C27B0"
    };

    /**
     * Registers a new user and returns a JWT.
     *
     * @param request registration payload
     * @return {@link AuthResponse} containing the JWT
     */
    public Mono<AuthResponse> register(RegisterRequest request) {
        return userRepository.existsByEmail(request.getEmail())
                .flatMap(emailExists -> {
                    if (emailExists) {
                        return Mono.error(new IllegalArgumentException("Email already registered"));
                    }
                    return userRepository.existsByUsername(request.getUsername());
                })
                .flatMap(usernameExists -> {
                    if (usernameExists) {
                        return Mono.error(new IllegalArgumentException("Username already taken"));
                    }
                    String avatarColor = AVATAR_COLORS[(int) (Math.random() * AVATAR_COLORS.length)];
                    User user = User.builder()
                            .email(request.getEmail())
                            .username(request.getUsername())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .displayName(request.getDisplayName())
                            .phone(request.getPhone())
                            .avatarColor(avatarColor)
                            .roles(List.of("USER"))
                            .build();
                    return userRepository.save(user);
                })
                .map(this::buildAuthResponse);
    }

    /**
     * Authenticates a user by email and password.
     *
     * @param request login payload
     * @return {@link AuthResponse} containing the JWT
     */
    public Mono<AuthResponse> login(AuthRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid email or password")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        return Mono.error(new BadCredentialsException("Invalid email or password"));
                    }
                    return Mono.just(user);
                })
                .map(this::buildAuthResponse);
    }

    /**
     * Finds a user by their email address.
     *
     * @param email the user's email
     * @return the {@link User} entity
     */
    public Mono<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getDisplayName());
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatarColor(user.getAvatarColor())
                .build();
    }
}
