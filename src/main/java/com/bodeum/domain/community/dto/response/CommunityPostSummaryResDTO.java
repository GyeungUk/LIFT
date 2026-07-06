package com.bodeum.domain.community.dto.response;

import com.bodeum.domain.community.model.CommunityPost;
import com.bodeum.domain.lifetransition.enumtype.LifeEventType;
import java.time.LocalDateTime;

public record CommunityPostSummaryResDTO(
        Long postId,
        LifeEventType category,
        String title,
        String contentPreview,
        String authorName,
        int likeCount,
        int commentCount,
        boolean liked,
        boolean mine,
        LocalDateTime createdAt
) {

    public static CommunityPostSummaryResDTO from(CommunityPost post, Long userId, boolean liked) {
        return new CommunityPostSummaryResDTO(
                post.getId(),
                post.getCategory(),
                post.getTitle(),
                preview(post.getContent()),
                "익명",
                post.getLikeCount(),
                post.getCommentCount(),
                liked,
                post.isAuthoredBy(userId),
                post.getCreatedAt()
        );
    }

    private static String preview(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 140 ? normalized : normalized.substring(0, 139) + "…";
    }
}
