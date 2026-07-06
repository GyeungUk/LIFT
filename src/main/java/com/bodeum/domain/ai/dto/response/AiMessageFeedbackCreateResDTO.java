package com.bodeum.domain.ai.dto.response;

import com.bodeum.domain.ai.enumtype.AiFeedbackType;
import com.bodeum.domain.ai.enumtype.AiFeedbackReasonType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiMessageFeedbackCreateResDTO(
        Long aiFeedbackId,
        AiFeedbackType feedbackType,
        List<AiFeedbackReasonType> reasons
) {
}
