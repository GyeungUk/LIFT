package com.lift.global.auth;

import com.lift.domain.auth.enumtype.SocialProvider;

public record AuthUserPrincipal(
        Long userId,
        SocialProvider provider,
        String nickname,
        String email
) {
}
