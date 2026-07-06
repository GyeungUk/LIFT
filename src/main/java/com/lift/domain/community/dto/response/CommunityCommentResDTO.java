package com.lift.domain.community.dto.response;

import com.lift.domain.community.model.CommunityComment;
import java.time.LocalDateTime;

public record CommunityCommentResDTO(
        Long commentId,
        String authorName,
        String content,
        boolean mine,
        LocalDateTime createdAt
) {

    public static CommunityCommentResDTO from(CommunityComment comment, Long userId) {
        return new CommunityCommentResDTO(
                comment.getId(),
                "익명",
                comment.getContent(),
                comment.isAuthoredBy(userId),
                comment.getCreatedAt()
        );
    }
}
