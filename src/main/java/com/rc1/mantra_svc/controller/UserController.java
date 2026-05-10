package com.rc1.mantra_svc.controller;

import com.rc1.mantra_svc.dto.chat.UserSearchResult;
import com.rc1.mantra_svc.repository.UserRepository;
import com.rc1.mantra_svc.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.regex.Pattern;

/**
 * User search endpoint — lets authenticated users find other people to
 * start a chat or add to a group.
 *
 * GET /api/users/search?q=alice — returns up to 20 matching users
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * Searches users by display name, email, or username.
     * Requires at least 2 characters to reduce noise.
     * The current user is excluded from results.
     *
     * @param q         search query (min 2 chars)
     * @param principal the authenticated caller
     * @return list of matching {@link UserSearchResult} (max 20)
     */
    @GetMapping("/search")
    public Mono<List<UserSearchResult>> search(
            @RequestParam String q,
            @AuthenticationPrincipal UserPrincipal principal) {

        String trimmed = q == null ? "" : q.trim();
        if (trimmed.length() < 2) {
            return Mono.just(List.of());
        }

        // Escape any regex meta-characters from user input to prevent ReDoS
        String safeQuery = Pattern.quote(trimmed);

        return userRepository.searchUsers(safeQuery)
                .filter(u -> !u.getId().equals(principal.getUserId()))
                .take(20)
                .map(u -> UserSearchResult.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .username(u.getUsername())
                        .displayName(u.getDisplayName() != null ? u.getDisplayName() : u.getUsername())
                        .avatarColor(u.getAvatarColor())
                        .build())
                .collectList();
    }
}
