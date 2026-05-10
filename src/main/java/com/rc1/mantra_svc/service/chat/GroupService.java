package com.rc1.mantra_svc.service.chat;

import com.rc1.mantra_svc.dto.chat.*;
import com.rc1.mantra_svc.exception.ResourceNotFoundException;
import com.rc1.mantra_svc.model.chat.Conversation;
import com.rc1.mantra_svc.model.chat.Group;
import com.rc1.mantra_svc.model.chat.Message;
import com.rc1.mantra_svc.model.chat.MessageStatus;
import com.rc1.mantra_svc.model.chat.MessageType;
import com.rc1.mantra_svc.repository.UserRepository;
import com.rc1.mantra_svc.repository.chat.GroupRepository;
import com.rc1.mantra_svc.repository.chat.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles group chat lifecycle: creation, membership, message delivery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatSessionRegistry registry;
    private final ConversationService conversationService;

    private static final int PAGE_SIZE = 30;

    public Flux<GroupDto> listGroups(String userId) {
        return groupRepository.findActiveGroupsByUserId(userId)
            .flatMap(group -> enrichGroupDto(group, userId));
    }

    public Mono<GroupDto> createGroup(String creatorId, CreateGroupRequest req) {
        // If a chat group already exists for this Splitwise expense group, reuse it.
        if (req.getLinkedExpenseGroupId() != null && !req.getLinkedExpenseGroupId().isBlank()) {
            return groupRepository.findFirstByLinkedExpenseGroupId(req.getLinkedExpenseGroupId())
                .flatMap(existing -> ensureMembership(existing, creatorId, req.getMemberIds()))
                .switchIfEmpty(Mono.defer(() -> doCreateGroup(creatorId, req)));
        }
        return doCreateGroup(creatorId, req);
    }

    private Mono<GroupDto> doCreateGroup(String creatorId, CreateGroupRequest req) {
        List<String> allMemberIds = new ArrayList<>();
        allMemberIds.add(creatorId);
        if (req.getMemberIds() != null) {
            req.getMemberIds().stream()
                .filter(id -> !id.equals(creatorId))
                .forEach(allMemberIds::add);
        }

        List<Group.GroupMember> members = allMemberIds.stream()
            .map(uid -> Group.GroupMember.builder()
                .userId(uid)
                .role(uid.equals(creatorId) ? Group.GroupRole.ADMIN : Group.GroupRole.MEMBER)
                .joinedAt(Instant.now())
                .build())
            .collect(Collectors.toList());

        Group group = Group.builder()
            .name(req.getName())
            .description(req.getDescription())
            .createdBy(creatorId)
            .linkedExpenseGroupId(req.getLinkedExpenseGroupId())
            .members(members)
            .unreadCounts(new HashMap<>())
            .build();

        return groupRepository.save(group)
            .flatMap(saved -> {
                // Send system message
                Message sysMsg = Message.builder()
                    .groupId(saved.getId())
                    .senderId(creatorId)
                    .type(MessageType.SYSTEM)
                    .content("Group created")
                    .status(MessageStatus.SENT)
                    .readReceipts(new ArrayList<>())
                    .build();
                return messageRepository.save(sysMsg).thenReturn(saved);
            })
            .flatMap(saved -> enrichGroupDto(saved, creatorId)
                .doOnSuccess(dto -> {
                    // Notify all initial members
                    ChatEvent<GroupDto> ev = ChatEvent.of(
                        ChatEvent.EventType.GROUP_UPDATED, saved.getId(), dto);
                    allMemberIds.forEach(uid -> registry.sendToUser(uid, ev));
                }));
    }

    /**
     * Add the requester (and any new requested members) to an existing group if
     * they're not already active members. Used when reusing a chat group linked
     * to a Splitwise expense group across accounts.
     */
    private Mono<GroupDto> ensureMembership(Group group, String requesterId, List<String> memberIds) {
        Set<String> active = group.getMembers().stream()
            .filter(m -> m.getLeftAt() == null)
            .map(Group.GroupMember::getUserId)
            .collect(Collectors.toSet());

        List<String> toAdd = new ArrayList<>();
        if (!active.contains(requesterId)) toAdd.add(requesterId);
        if (memberIds != null) {
            memberIds.stream()
                .filter(id -> id != null && !id.isBlank() && !active.contains(id) && !toAdd.contains(id))
                .forEach(toAdd::add);
        }

        if (toAdd.isEmpty()) return enrichGroupDto(group, requesterId);

        // Re-activate previously-left members or add fresh ones.
        toAdd.forEach(uid -> {
            Optional<Group.GroupMember> existing = group.getMembers().stream()
                .filter(m -> m.getUserId().equals(uid))
                .findFirst();
            if (existing.isPresent()) {
                existing.get().setLeftAt(null);
            } else {
                group.getMembers().add(Group.GroupMember.builder()
                    .userId(uid)
                    .role(Group.GroupRole.MEMBER)
                    .joinedAt(Instant.now())
                    .build());
            }
        });

        return groupRepository.save(group)
            .flatMap(saved -> enrichGroupDto(saved, requesterId)
                .doOnSuccess(dto -> {
                    ChatEvent<GroupDto> ev = ChatEvent.of(
                        ChatEvent.EventType.GROUP_UPDATED, saved.getId(), dto);
                    saved.getMembers().stream()
                        .filter(m -> m.getLeftAt() == null)
                        .forEach(m -> registry.sendToUser(m.getUserId(), ev));
                }));
    }

    public Mono<GroupDto> addMember(String groupId, String requesterId, String newMemberId) {
        return groupRepository.findById(groupId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Group not found")))
            .flatMap(group -> {
                assertAdmin(group, requesterId);
                boolean alreadyMember = group.getMembers().stream()
                    .anyMatch(m -> m.getUserId().equals(newMemberId) && m.getLeftAt() == null);
                if (alreadyMember) return enrichGroupDto(group, requesterId);

                group.getMembers().add(Group.GroupMember.builder()
                    .userId(newMemberId)
                    .role(Group.GroupRole.MEMBER)
                    .joinedAt(Instant.now())
                    .build());

                return groupRepository.save(group)
                    .flatMap(saved -> {
                        // System message
                        return userRepository.findById(newMemberId)
                            .flatMap(u -> {
                                Message sysMsg = Message.builder()
                                    .groupId(groupId)
                                    .senderId(requesterId)
                                    .type(MessageType.SYSTEM)
                                    .content(u.getDisplayName() + " joined the group")
                                    .status(MessageStatus.SENT)
                                    .readReceipts(new ArrayList<>())
                                    .build();
                                return messageRepository.save(sysMsg).thenReturn(saved);
                            });
                    })
                    .flatMap(saved -> enrichGroupDto(saved, requesterId)
                        .doOnSuccess(dto -> {
                            ChatEvent<GroupDto> ev = ChatEvent.of(
                                ChatEvent.EventType.GROUP_MEMBER_ADDED, groupId, dto);
                            saved.getMembers().stream()
                                .filter(m -> m.getLeftAt() == null)
                                .forEach(m -> registry.sendToUser(m.getUserId(), ev));
                        }));
            });
    }

    public Mono<Void> removeMember(String groupId, String requesterId, String targetUserId) {
        return groupRepository.findById(groupId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Group not found")))
            .flatMap(group -> {
                // Admins can remove anyone; members can only remove themselves
                boolean isSelf = requesterId.equals(targetUserId);
                if (!isSelf) assertAdmin(group, requesterId);

                group.getMembers().stream()
                    .filter(m -> m.getUserId().equals(targetUserId))
                    .findFirst()
                    .ifPresent(m -> m.setLeftAt(Instant.now()));

                return groupRepository.save(group)
                    .flatMap(saved -> {
                        return userRepository.findById(targetUserId)
                            .flatMap(u -> {
                                Message sysMsg = Message.builder()
                                    .groupId(groupId)
                                    .senderId(requesterId)
                                    .type(MessageType.SYSTEM)
                                    .content(u.getDisplayName() + (isSelf ? " left the group" : " was removed"))
                                    .status(MessageStatus.SENT)
                                    .readReceipts(new ArrayList<>())
                                    .build();
                                return messageRepository.save(sysMsg).thenReturn(saved);
                            });
                    })
                    .flatMap(saved -> enrichGroupDto(saved, requesterId)
                        .doOnSuccess(dto -> {
                            ChatEvent<GroupDto> ev = ChatEvent.of(
                                ChatEvent.EventType.GROUP_MEMBER_REMOVED, groupId, dto);
                            saved.getMembers().forEach(m -> registry.sendToUser(m.getUserId(), ev));
                            registry.sendToUser(targetUserId, ev);
                        }))
                    .then();
            });
    }

    public Mono<MessageDto> sendGroupMessage(String senderId, SendMessageRequest req) {
        return groupRepository.findById(req.getGroupId())
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Group not found")))
            .flatMap(group -> {
                boolean isMember = group.getMembers().stream()
                    .anyMatch(m -> m.getUserId().equals(senderId) && m.getLeftAt() == null);
                if (!isMember) return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));

                Message msg = Message.builder()
                    .groupId(group.getId())
                    .senderId(senderId)
                    .type(req.getType())
                    .content(req.getContent())
                    .status(MessageStatus.SENT)
                    .replyToMessageId(req.getReplyToMessageId())
                    .readReceipts(new ArrayList<>())
                    .build();

                return messageRepository.save(msg)
                    .flatMap(saved -> {
                        group.setLastMessage(Conversation.MessageSummary.builder()
                            .messageId(saved.getId())
                            .senderId(senderId)
                            .content(req.getType() == MessageType.TEXT ? req.getContent() : "[" + req.getType() + "]")
                            .type(req.getType())
                            .sentAt(saved.getCreatedAt())
                            .build());
                        group.getMembers().stream()
                            .filter(m -> m.getLeftAt() == null && !m.getUserId().equals(senderId))
                            .forEach(m -> group.getUnreadCounts().merge(m.getUserId(), 1, Integer::sum));

                        return groupRepository.save(group).thenReturn(saved);
                    });
            })
            .flatMap(msg -> conversationService.enrichMessageDto(msg)
                .doOnSuccess(dto -> {
                    ChatEvent<MessageDto> ev = ChatEvent.of(
                        ChatEvent.EventType.NEW_MESSAGE, req.getGroupId(), dto);
                    groupRepository.findById(req.getGroupId())
                        .subscribe(group -> group.getMembers().stream()
                            .filter(m -> m.getLeftAt() == null)
                            .forEach(m -> registry.sendToUser(m.getUserId(), ev)));
                }));
    }

    public Flux<MessageDto> getGroupMessageHistory(String groupId, String before, int page) {
        PageRequest pageReq = PageRequest.of(page, PAGE_SIZE);
        Flux<Message> msgs = before != null
            ? messageRepository.findByGroupIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                groupId, Instant.parse(before), pageReq)
            : messageRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageReq);

        return msgs.flatMap(conversationService::enrichMessageDto);
    }

    public Mono<Void> markGroupRead(String groupId, String userId) {
        return groupRepository.findById(groupId)
            .flatMap(group -> {
                group.getUnreadCounts().put(userId, 0);
                return groupRepository.save(group);
            })
            .then();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void assertAdmin(Group group, String userId) {
        boolean isAdmin = group.getMembers().stream()
            .anyMatch(m -> m.getUserId().equals(userId)
                && m.getRole() == Group.GroupRole.ADMIN
                && m.getLeftAt() == null);
        if (!isAdmin) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
    }

    private Mono<GroupDto> enrichGroupDto(Group group, String requesterId) {
        List<String> memberIds = group.getMembers().stream()
            .filter(m -> m.getLeftAt() == null)
            .map(Group.GroupMember::getUserId)
            .collect(Collectors.toList());

        return userRepository.findAllById(memberIds).collectList()
            .flatMap(users -> {
                Map<String, com.rc1.mantra_svc.model.User> userMap = users.stream()
                    .collect(Collectors.toMap(u -> u.getId(), u -> u));

                List<GroupDto.GroupMemberDto> memberDtos = group.getMembers().stream()
                    .filter(m -> m.getLeftAt() == null)
                    .map(m -> {
                        com.rc1.mantra_svc.model.User u = userMap.get(m.getUserId());
                        return GroupDto.GroupMemberDto.builder()
                            .userId(m.getUserId())
                            .displayName(u != null ? u.getDisplayName() : "Unknown")
                            .username(u != null ? u.getUsername() : "")
                            .avatarColor(u != null ? u.getAvatarColor() : "#6C63FF")
                            .role(m.getRole())
                            .online(registry.isOnline(m.getUserId()))
                            .joinedAt(m.getJoinedAt())
                            .build();
                    })
                    .collect(Collectors.toList());

                return Mono.just(GroupDto.builder()
                    .id(group.getId())
                    .name(group.getName())
                    .description(group.getDescription())
                    .avatarUrl(group.getAvatarUrl())
                    .createdBy(group.getCreatedBy())
                    .linkedExpenseGroupId(group.getLinkedExpenseGroupId())
                    .members(memberDtos)
                    .lastMessage(group.getLastMessage())
                    .unreadCount(group.getUnreadCounts().getOrDefault(requesterId, 0))
                    .updatedAt(group.getUpdatedAt())
                    .build());
            });
    }
}
