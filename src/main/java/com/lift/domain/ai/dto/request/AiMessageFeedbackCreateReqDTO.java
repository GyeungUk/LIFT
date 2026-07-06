package com.lift.domain.ai.dto.request;

import com.lift.domain.ai.enumtype.AiFeedbackType;
import com.lift.domain.ai.enumtype.AiFeedbackReasonType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiMessageFeedbackCreateReqDTO(
        @NotNull(message = "피드백 유형은 필수입니다.")
        AiFeedbackType feedbackType,

        @Size(min = 1, message = "사유를 하나 이상 선택해주세요")
        List<AiFeedbackReasonType> reasons
) {
}
