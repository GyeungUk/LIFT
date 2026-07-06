package com.lift.domain.lifetransition.dto.response;

import com.lift.domain.lifetransition.enumtype.ChatSenderType;
import com.lift.domain.lifetransition.model.ReportChatMessage;
import java.time.LocalDateTime;

public record ReportChatMessageResDTO(
        Long messageId,
        ChatSenderType senderType,
        String content,
        LocalDateTime createdAt
) {

    public static ReportChatMessageResDTO from(ReportChatMessage message) {
        return new ReportChatMessageResDTO(
                message.getId(),
                message.getSenderType(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
