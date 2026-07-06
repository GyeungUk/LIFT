package com.lift.domain.lifetransition.dto.response;

import com.lift.domain.lifetransition.enumtype.AssessmentStatus;
import com.lift.domain.lifetransition.enumtype.PaymentStatus;
import com.lift.domain.lifetransition.model.LifeReport;

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
