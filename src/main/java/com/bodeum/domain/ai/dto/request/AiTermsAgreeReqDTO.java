package com.bodeum.domain.ai.dto.request;

import jakarta.validation.constraints.AssertTrue;

public record AiTermsAgreeReqDTO(
        @AssertTrue(message = "AI 챗봇 이용동의가 필요합니다.")
        boolean aiTermsAgreed
) {
}
