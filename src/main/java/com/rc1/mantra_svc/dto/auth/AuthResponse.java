package com.rc1.mantra_svc.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** JWT authentication response returned to the client after login/register. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String userId;
    private String email;
    private String username;
    private String displayName;
    private String avatarColor;
}
