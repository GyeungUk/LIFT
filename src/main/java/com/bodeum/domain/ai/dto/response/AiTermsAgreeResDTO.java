package com.bodeum.domain.ai.dto.response;

import java.time.LocalDateTime;

public record AiTermsAgreeResDTO(
        LocalDateTime agreedAt
) {
}
