package com.rc1.mantra_svc.controller;

import com.rc1.mantra_svc.dto.ApiResponse;
import com.rc1.mantra_svc.dto.auth.AuthRequest;
import com.rc1.mantra_svc.dto.auth.AuthResponse;
import com.rc1.mantra_svc.dto.auth.RegisterRequest;
import com.rc1.mantra_svc.model.User;
import com.rc1.mantra_svc.security.UserPrincipal;
import com.rc1.mantra_svc.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Authentication endpoints — register, login, and current-user lookup.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Registers a new user and returns a JWT. */
    @PostMapping("/register")
    public Mono<ResponseEntity<ApiResponse<AuthResponse>>> register(
            @Valid @RequestBody RegisterRequest request) {
        return authService.register(request)
                .map(resp -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(ApiResponse.success("Registration successful", resp)));
    }

    /** Authenticates a user and returns a JWT. */
    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<AuthResponse>>> login(
            @Valid @RequestBody AuthRequest request) {
        return authService.login(request)
                .map(resp -> ResponseEntity.ok(ApiResponse.success(resp)));
    }

    /** Returns the profile of the currently authenticated user. */
    @GetMapping("/me")
    public Mono<ResponseEntity<ApiResponse<User>>> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (UserPrincipal) ctx.getAuthentication().getPrincipal())
                .flatMap(principal -> authService.getUserByEmail(principal.getEmail()))
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)));
    }
}
