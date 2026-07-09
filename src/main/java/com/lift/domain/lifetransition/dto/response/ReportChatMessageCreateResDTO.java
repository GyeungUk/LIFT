package com.lift.domain.lifetransition.dto.response;

import com.lift.domain.lifetransition.model.LifeReport;
import com.lift.domain.lifetransition.model.ReportChatMessage;

/**
 * AI 질문 1회에 대한 응답. 저장된 사용자 메시지와 AI 응답, 남은 질문 횟수를 함께 반환한다.
 */
public record ReportChatMessageCreateResDTO(
        ReportChatMessageResDTO userMessage,
        ReportChatMessageResDTO aiMessage,
        int aiQuestionLimit,
        int aiQuestionUsedCount,
        int aiQuestionRemaining
) {

    public static ReportChatMessageCreateResDTO of(
            LifeReport report,
            ReportChatMessage userMessage,
            ReportChatMessage aiMessage
    ) {
        return new ReportChatMessageCreateResDTO(
                ReportChatMessageResDTO.from(userMessage),
                ReportChatMessageResDTO.from(aiMessage),
                report.getAiQuestionLimit(),
                report.getAiQuestionUsedToday(),
                report.getAiQuestionRemaining()
        );
    }
}
