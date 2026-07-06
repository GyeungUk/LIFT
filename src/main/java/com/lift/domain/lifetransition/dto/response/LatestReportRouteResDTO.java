package com.lift.domain.lifetransition.dto.response;

import com.lift.domain.lifetransition.enumtype.PaymentStatus;
import com.lift.domain.lifetransition.model.LifeReport;

/**
 * 로그인 직후 기존 사용자를 어디로 보낼지 판단하는 최신 리포트 상태.
 */
public record LatestReportRouteResDTO(
        boolean available,
        Long reportId,
        PaymentStatus paymentStatus
) {

    public static LatestReportRouteResDTO from(LifeReport report) {
        return new LatestReportRouteResDTO(true, report.getId(), report.getPaymentStatus());
    }

    public static LatestReportRouteResDTO empty() {
        return new LatestReportRouteResDTO(false, null, null);
    }
}
