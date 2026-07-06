package com.lift.domain.auth.service;

public record SocialUserProfile(
        String providerUserId,
        String email,
        String nickname
) {
}
