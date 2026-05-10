package com.rc1.mantra_svc.security;

import com.rc1.mantra_svc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Reactive UserDetails service that loads a {@link UserPrincipal}
 * from MongoDB by email (used as the JWT subject / username).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements ReactiveUserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads user by email and maps it to a {@link UserPrincipal}.
     *
     * @param email the JWT subject
     * @return {@link Mono} of {@link UserDetails}
     */
    @Override
    public Mono<UserDetails> findByUsername(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found: " + email)))
                .map(user -> {
                    List<SimpleGrantedAuthority> authorities = user.getRoles() != null
                            ? user.getRoles().stream()
                                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                                    .collect(Collectors.toList())
                            : List.of(new SimpleGrantedAuthority("ROLE_USER"));

                    return (UserDetails) new UserPrincipal(
                            user.getId(),
                            user.getEmail(),
                            user.getPassword(),
                            user.getDisplayName(),
                            authorities
                    );
                });
    }
}
