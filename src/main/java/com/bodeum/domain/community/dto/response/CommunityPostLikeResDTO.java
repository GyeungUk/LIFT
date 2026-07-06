package com.bodeum.domain.community.dto.response;

public record CommunityPostLikeResDTO(
        Long postId,
        int likeCount,
        boolean liked
) {
}
