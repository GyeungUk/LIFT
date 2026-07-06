package com.lift.domain.ai.dto.response;

import com.lift.domain.ai.enumtype.AiFeedbackType;
import com.lift.domain.ai.enumtype.AiFeedbackReasonType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiMessageFeedbackCreateResDTO(
        Long aiFeedbackId,
        AiFeedbackType feedbackType,
        List<AiFeedbackReasonType> reasons
) {
}
