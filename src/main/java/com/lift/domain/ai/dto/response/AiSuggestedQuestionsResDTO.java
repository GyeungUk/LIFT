package com.lift.domain.ai.dto.response;

import java.util.List;

public record AiSuggestedQuestionsResDTO(
        List<String> questions
) {
}
