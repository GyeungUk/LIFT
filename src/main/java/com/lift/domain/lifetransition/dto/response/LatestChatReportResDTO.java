package com.lift.domain.lifetransition.dto.response;

import com.lift.domain.lifetransition.model.LifeReport;

/**
 * 상단 AI챗봇 진입점에서 사용할 최신 결제 리포트 정보.
 */
public record LatestChatReportResDTO(
        boolean available,
        Long reportId
) {

    public static LatestChatReportResDTO from(LifeReport report) {
        return new LatestChatReportResDTO(true, report.getId());
    }

    public static LatestChatReportResDTO empty() {
        return new LatestChatReportResDTO(false, null);
    }
}
