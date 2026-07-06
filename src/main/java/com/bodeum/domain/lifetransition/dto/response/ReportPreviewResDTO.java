package com.bodeum.domain.lifetransition.dto.response;

import com.bodeum.domain.lifetransition.enumtype.EligibilityLevel;
import com.bodeum.domain.lifetransition.enumtype.PaymentStatus;
import com.bodeum.domain.lifetransition.enumtype.PriorityLevel;
import com.bodeum.domain.lifetransition.enumtype.ProcedureType;
import com.bodeum.domain.lifetransition.model.LifeReport;
import com.bodeum.domain.lifetransition.model.ReportItem;
import java.util.List;

/**
 * 결제 전 미리보기. 전체 항목 수와 가장 중요한 1~2개 항목의 요약, 결제 유도 문구만 노출한다.
 * 상세 사유/서류/공식 링크 등 유료 콘텐츠는 포함하지 않는다.
 */
public record ReportPreviewResDTO(
        Long reportId,
        String summaryTitle,
        String summaryMessage,
        int totalItemCount,
        PaymentStatus paymentStatus,
        boolean locked,
        List<HighlightItem> highlightItems,
        String ctaMessage
) {

    private static final int HIGHLIGHT_LIMIT = 2;

    public record HighlightItem(
            ProcedureType procedureType,
            String procedureName,
            String title,
            EligibilityLevel eligibilityLevel,
            PriorityLevel priorityLevel
    ) {
        private static HighlightItem from(ReportItem item) {
            return new HighlightItem(
                    item.getProcedureType(),
                    item.getProcedureType().getDisplayName(),
                    item.getTitle(),
                    item.getEligibilityLevel(),
                    item.getPriorityLevel()
            );
        }
    }

    public static ReportPreviewResDTO from(LifeReport report) {
        int totalItemCount = report.getItems().size();

        List<HighlightItem> highlights = report.getItems().stream()
                .limit(HIGHLIGHT_LIMIT)
                .map(HighlightItem::from)
                .toList();

        int hiddenCount = Math.max(0, totalItemCount - highlights.size());
        String cta = hiddenCount > 0
                ? "나머지 " + hiddenCount + "개 항목의 상세 사유·필요 서류·공식 신청 링크는 결제 후 확인할 수 있어요."
                : "상세 사유·필요 서류·공식 신청 링크는 결제 후 확인할 수 있어요.";

        return new ReportPreviewResDTO(
                report.getId(),
                report.getSummaryTitle(),
                report.getSummaryMessage(),
                totalItemCount,
                report.getPaymentStatus(),
                !report.isPaid(),
                highlights,
                cta
        );
    }
}
