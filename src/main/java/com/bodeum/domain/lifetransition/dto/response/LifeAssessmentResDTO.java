package com.bodeum.domain.lifetransition.dto.response;

import com.bodeum.domain.lifetransition.enumtype.AssessmentStatus;
import com.bodeum.domain.lifetransition.enumtype.LifeEventType;
import com.bodeum.domain.lifetransition.model.LifeAssessment;
import java.time.LocalDateTime;

public record LifeAssessmentResDTO(
        Long assessmentId,
        LifeEventType eventType,
        AssessmentStatus status,
        LocalDateTime createdAt
) {

    public static LifeAssessmentResDTO from(LifeAssessment assessment) {
        return new LifeAssessmentResDTO(
                assessment.getId(),
                assessment.getEventType(),
                assessment.getStatus(),
                assessment.getCreatedAt()
        );
    }
}
