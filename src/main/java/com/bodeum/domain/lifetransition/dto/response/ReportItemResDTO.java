package com.bodeum.domain.lifetransition.dto.response;

import com.bodeum.domain.lifetransition.enumtype.EligibilityLevel;
import com.bodeum.domain.lifetransition.enumtype.PriorityLevel;
import com.bodeum.domain.lifetransition.enumtype.ProcedureType;
import com.bodeum.domain.lifetransition.model.ReportItem;
import java.util.List;

public record ReportItemResDTO(
        Long itemId,
        ProcedureType procedureType,
        String procedureName,
        EligibilityLevel eligibilityLevel,
        PriorityLevel priorityLevel,
        String title,
        String reason,
        String deadlineText,
        String officialUrl,
        int sortOrder,
        BenefitEstimateResDTO estimate,
        List<RequiredDocumentResDTO> requiredDocuments
) {

    public static ReportItemResDTO from(ReportItem item) {
        return from(item, null);
    }

    public static ReportItemResDTO from(ReportItem item, BenefitEstimateResDTO estimate) {
        return new ReportItemResDTO(
                item.getId(),
                item.getProcedureType(),
                item.getProcedureType().getDisplayName(),
                item.getEligibilityLevel(),
                item.getPriorityLevel(),
                item.getTitle(),
                item.getReason(),
                item.getDeadlineText(),
                item.getOfficialUrl(),
                item.getSortOrder(),
                estimate,
                item.getRequiredDocuments().stream()
                        .map(RequiredDocumentResDTO::from)
                        .toList()
        );
    }
}
