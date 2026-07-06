package com.bodeum.domain.lifetransition.dto.response;

import com.bodeum.domain.lifetransition.model.LifeReport;
import com.bodeum.domain.lifetransition.model.ReportChatMessage;
import java.util.List;

/**
 * 리포트 기반 채팅 이력 조회 응답.
 */
public record ReportChatMessagesResDTO(
        List<ReportChatMessageResDTO> messages,
        int aiQuestionLimit,
        int aiQuestionUsedCount,
        int aiQuestionRemaining
) {

    public static ReportChatMessagesResDTO of(LifeReport report, List<ReportChatMessage> messages) {
        return new ReportChatMessagesResDTO(
                messages.stream()
                        .map(ReportChatMessageResDTO::from)
                        .toList(),
                report.getAiQuestionLimit(),
                report.getAiQuestionUsedCount(),
                report.getAiQuestionRemaining()
        );
    }
}
