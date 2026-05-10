package com.rc1.mantra_svc.service.chat;

import com.rc1.mantra_svc.dto.chat.ChatEvent;
import com.rc1.mantra_svc.dto.chat.PresenceEvent;
import com.rc1.mantra_svc.model.chat.UserPresence;
import com.rc1.mantra_svc.repository.chat.ConversationRepository;
import com.rc1.mantra_svc.repository.chat.GroupRepository;
import com.rc1.mantra_svc.repository.chat.PresenceRepository;
import com.rc1.mantra_svc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Tracks and broadcasts user online/offline presence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final PresenceRepository presenceRepository;
    private final ConversationRepository conversationRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ChatSessionRegistry registry;

    /**
     * Mark a user as online and notify all their conversation partners.
     */
    public Mono<Void> markOnline(String userId, String sessionId) {
        UserPresence presence = UserPresence.builder()
            .userId(userId)
            .online(true)
            .sessionId(sessionId)
            .lastSeenAt(Instant.now())
            .build();

        return presenceRepository.save(presence)
            .then(broadcastPresence(userId, true));
    }

    /**
     * Mark a user as offline and notify all their conversation partners.
     */
    public Mono<Void> markOffline(String userId) {
        return presenceRepository.findById(userId)
            .flatMap(p -> {
                p.setOnline(false);
                p.setLastSeenAt(Instant.now());
                return presenceRepository.save(p);
            })
            .switchIfEmpty(Mono.defer(() -> {
                UserPresence p = UserPresence.builder()
                    .userId(userId).online(false).lastSeenAt(Instant.now()).build();
                return presenceRepository.save(p);
            }))
            .then(broadcastPresence(userId, false));
    }

    public Mono<Boolean> isOnline(String userId) {
        return Mono.just(registry.isOnline(userId));
    }

    public Flux<UserPresence> getPresenceForUsers(List<String> userIds) {
        return presenceRepository.findByUserIdIn(userIds);
    }

    private Mono<Void> broadcastPresence(String userId, boolean online) {
        return userRepository.findById(userId)
            .flatMap(user -> {
                PresenceEvent event = PresenceEvent.builder()
                    .userId(userId)
                    .displayName(user.getDisplayName())
                    .online(online)
                    .lastSeenAt(Instant.now())
                    .build();
                ChatEvent<PresenceEvent> chatEvent =
                    ChatEvent.of(ChatEvent.EventType.PRESENCE_UPDATE, userId, event);

                // Notify everyone in direct conversations
                Mono<Void> convNotify = conversationRepository.findByParticipantId(userId)
                    .flatMap(conv -> Flux.fromIterable(conv.getParticipantIds())
                        .filter(pid -> !pid.equals(userId))
                        .doOnNext(pid -> registry.sendToUser(pid, chatEvent))
                        .then())
                    .then();

                // Notify group members
                Mono<Void> groupNotify = groupRepository.findActiveGroupsByUserId(userId)
                    .flatMap(group -> Flux.fromIterable(group.getMembers())
                        .filter(m -> m.getLeftAt() == null && !m.getUserId().equals(userId))
                        .doOnNext(m -> registry.sendToUser(m.getUserId(), chatEvent))
                        .then())
                    .then();

                return convNotify.then(groupNotify);
            })
            .onErrorResume(e -> {
                log.error("Error broadcasting presence for {}: {}", userId, e.getMessage());
                return Mono.empty();
            });
    }
}
