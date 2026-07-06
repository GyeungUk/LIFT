package com.bodeum.domain.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record AiChatRoomResDTO(
        Long aiChatRoomId,
        LocalDateTime createdAt,
        @JsonProperty("isFirstVisit")
        boolean isFirstVisit
) {
}
