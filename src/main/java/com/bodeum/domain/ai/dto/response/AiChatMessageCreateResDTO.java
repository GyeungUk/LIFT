package com.bodeum.domain.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

public record AiChatMessageCreateResDTO(
        MessageDTO userMessage,
        MessageDTO aiMessage,
        List<String> followUpQuestions
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MessageDTO(
            Long aiMessageId,
            String content,
            LocalDateTime createdAt,
            List<ResponseSourceDTO> responseSource
    ) {
    }

    public record ResponseSourceDTO(
            String sourceTitle,
            String sourceUrl,
            Long infoId
    ) {
    }
}
