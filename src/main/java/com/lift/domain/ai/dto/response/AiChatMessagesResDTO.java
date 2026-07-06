package com.lift.domain.ai.dto.response;

import com.lift.domain.ai.enumtype.AiFeedbackType;
import com.lift.domain.ai.enumtype.AiMessageSenderType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

public record AiChatMessagesResDTO(
        List<MessageDTO> messages,
        boolean hasNext,
        Long nextCursor
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MessageDTO(
            Long aiMessageId,
            AiMessageSenderType senderType,
            String content,
            LocalDateTime createdAt,
            ResponseSourceDTO responseSource,
            FeedbackDTO feedback
    ) {
    }

    public record ResponseSourceDTO(
            String sourceTitle,
            String sourceUrl
    ) {
    }

    public record FeedbackDTO(
            AiFeedbackType feedbackType
    ) {
    }
}
