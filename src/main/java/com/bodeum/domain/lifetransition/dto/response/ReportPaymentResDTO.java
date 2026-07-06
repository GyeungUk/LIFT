package com.bodeum.domain.lifetransition.dto.response;

import com.bodeum.domain.lifetransition.enumtype.AssessmentStatus;
import com.bodeum.domain.lifetransition.enumtype.PaymentStatus;
import com.bodeum.domain.lifetransition.model.LifeReport;

/**
 * 결제 mock 완료 응답.
 */
public record ReportPaymentResDTO(
        Long reportId,
        PaymentStatus paymentStatus,
        AssessmentStatus assessmentStatus
) {

    public static ReportPaymentResDTO from(LifeReport report) {
        return new ReportPaymentResDTO(
                report.getId(),
                report.getPaymentStatus(),
                report.getAssessment().getStatus()
        );
    }
}
