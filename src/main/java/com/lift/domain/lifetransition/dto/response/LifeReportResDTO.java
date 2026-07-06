package com.lift.domain.lifetransition.dto.response;

import com.lift.domain.lifetransition.enumtype.PaymentStatus;
import com.lift.domain.lifetransition.enumtype.ProcedureType;
import com.lift.domain.lifetransition.model.LifeReport;
import com.lift.domain.lifetransition.service.BenefitEstimationService.ReportEstimation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 결제 완료 후 공개되는 상세 리포트. PDF 다운로드용 데이터로도 사용된다.
 */
public record LifeReportResDTO(
        Long reportId,
        Long assessmentId,
        String summaryTitle,
        String summaryMessage,
        int totalPriorityScore,
        PaymentStatus paymentStatus,
        int aiQuestionLimit,
        int aiQuestionUsedCount,
        int aiQuestionRemaining,
        LocalDateTime createdAt,
        BenefitSummaryResDTO benefitSummary,
        List<ReportItemResDTO> items,
        List<PublicBenefitResDTO> publicBenefits
) {

    public static LifeReportResDTO from(LifeReport report, ReportEstimation estimation) {
        return from(report, estimation, List.of());
    }

    public static LifeReportResDTO from(
            LifeReport report,
            ReportEstimation estimation,
            List<PublicBenefitResDTO> publicBenefits
    ) {
        Map<ProcedureType, BenefitEstimateResDTO> estimates = estimation.perItem();
        return new LifeReportResDTO(
                report.getId(),
                report.getAssessment().getId(),
                report.getSummaryTitle(),
                report.getSummaryMessage(),
                report.getTotalPriorityScore(),
                report.getPaymentStatus(),
                report.getAiQuestionLimit(),
                report.getAiQuestionUsedCount(),
                report.getAiQuestionRemaining(),
                report.getCreatedAt(),
                estimation.summary(),
                report.getItems().stream()
                        .map(item -> ReportItemResDTO.from(item, estimates.get(item.getProcedureType())))
                        .toList(),
                publicBenefits == null ? List.of() : publicBenefits
        );
    }
}
