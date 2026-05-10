package com.rc1.mantra_svc.controller;

import com.rc1.mantra_svc.dto.chat.CreateGroupRequest;
import com.rc1.mantra_svc.dto.chat.GroupDto;
import com.rc1.mantra_svc.dto.chat.MessageDto;
import com.rc1.mantra_svc.dto.chat.SendMessageRequest;
import com.rc1.mantra_svc.security.UserPrincipal;
import com.rc1.mantra_svc.service.chat.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * REST API for group chats.
 *
 * GET    /api/chat/groups                          — list joined groups
 * POST   /api/chat/groups                          — create group
 * GET    /api/chat/groups/{id}/messages            — paginated history
 * POST   /api/chat/groups/{id}/messages            — send message to group
 * POST   /api/chat/groups/{id}/members             — add member (admin)
 * DELETE /api/chat/groups/{id}/members/{userId}    — remove member (admin or self)
 * PUT    /api/chat/groups/{id}/read                — mark read
 */
@RestController
@RequestMapping("/api/chat/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public Mono<List<GroupDto>> listGroups(@AuthenticationPrincipal UserPrincipal principal) {
        return groupService.listGroups(principal.getUserId()).collectList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<GroupDto> createGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateGroupRequest req) {
        return groupService.createGroup(principal.getUserId(), req);
    }

    @GetMapping("/{id}/messages")
    public Mono<List<MessageDto>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @RequestParam(required = false) String before,
            @RequestParam(defaultValue = "0") int page) {
        return groupService.getGroupMessageHistory(id, before, page).collectList();
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MessageDto> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody SendMessageRequest req) {
        req.setGroupId(id);
        return groupService.sendGroupMessage(principal.getUserId(), req);
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> addMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        return groupService.addMember(id, principal.getUserId(), body.get("userId")).then();
    }

    @DeleteMapping("/{id}/members/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @PathVariable String targetUserId) {
        return groupService.removeMember(id, principal.getUserId(), targetUserId);
    }

    /**
     * Leave / hide a group for the current user. Equivalent to
     * {@code removeMember(id, self, self)} — the user is marked as left and
     * the group disappears from their list.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> leaveGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return groupService.removeMember(id, principal.getUserId(), principal.getUserId());
    }

    @PutMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return groupService.markGroupRead(id, principal.getUserId());
    }
}
