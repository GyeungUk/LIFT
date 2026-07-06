package com.lift.domain.lifetransition.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 리포트 기반 AI 질문 요청.
 */
public record ReportChatMessageCreateReqDTO(
        @NotBlank(message = "질문 내용을 입력해주세요.")
        @Size(max = 2000, message = "질문은 최대 2000자까지 입력 가능합니다.")
        String content
) {
}
