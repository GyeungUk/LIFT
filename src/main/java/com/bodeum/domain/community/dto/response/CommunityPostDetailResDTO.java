package com.bodeum.domain.community.dto.response;

import com.bodeum.domain.community.model.CommunityPost;
import com.bodeum.domain.lifetransition.enumtype.LifeEventType;
import java.time.LocalDateTime;
import java.util.List;

public record CommunityPostDetailResDTO(
        Long postId,
        LifeEventType category,
        String title,
        String content,
        String authorName,
        int likeCount,
        int commentCount,
        boolean liked,
        boolean mine,
        LocalDateTime createdAt,
        List<CommunityCommentResDTO> comments
) {

    public static CommunityPostDetailResDTO from(
            CommunityPost post,
            Long userId,
            boolean liked,
            List<CommunityCommentResDTO> comments
    ) {
        return new CommunityPostDetailResDTO(
                post.getId(),
                post.getCategory(),
                post.getTitle(),
                post.getContent(),
                "익명",
                post.getLikeCount(),
                post.getCommentCount(),
                liked,
                post.isAuthoredBy(userId),
                post.getCreatedAt(),
                comments
        );
    }
}
