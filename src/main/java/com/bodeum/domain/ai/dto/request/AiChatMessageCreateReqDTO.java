package com.bodeum.domain.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatMessageCreateReqDTO(
        @NotBlank(message = "질문 내용은 필수입니다.")
        @Size(max = 500, message = "메시지는 최대 500자까지 입력 가능합니다.")
        String content
) {
}
