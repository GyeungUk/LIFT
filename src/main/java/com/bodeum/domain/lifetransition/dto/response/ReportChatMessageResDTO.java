package com.bodeum.domain.lifetransition.dto.response;

import com.bodeum.domain.lifetransition.enumtype.ChatSenderType;
import com.bodeum.domain.lifetransition.model.ReportChatMessage;
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
