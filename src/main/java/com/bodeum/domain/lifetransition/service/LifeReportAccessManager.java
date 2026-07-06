package com.bodeum.domain.lifetransition.service;

import com.bodeum.domain.lifetransition.exception.LifeTransitionErrorCode;
import com.bodeum.domain.lifetransition.model.LifeReport;
import com.bodeum.domain.lifetransition.repository.LifeReportRepository;
import com.bodeum.domain.user.model.UserAccount;
import com.bodeum.domain.user.service.UserService;
import com.bodeum.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 리포트 접근 시 소유권/결제 상태를 공통 검증한다.
 */
@Component
@RequiredArgsConstructor
public class LifeReportAccessManager {

    private final LifeReportRepository lifeReportRepository;
    private final UserService userService;

    /**
     * 현재 사용자가 소유한 리포트를 조회한다. 없으면 404, 남의 리포트면 403.
     */
    public LifeReport getOwnedReport(Authentication authentication, Long reportId) {
        UserAccount user = userService.getCurrentUser(authentication);
        LifeReport report = lifeReportRepository.findById(reportId)
                .orElseThrow(() -> new ProjectException(LifeTransitionErrorCode.REPORT_NOT_FOUND));

        if (!report.isOwnedBy(user.getId())) {
            throw new ProjectException(LifeTransitionErrorCode.REPORT_ACCESS_DENIED);
        }

        return report;
    }

    /**
     * 소유권 확인 + 결제 완료(PAID) 여부까지 검증한다. 미결제면 403.
     */
    public LifeReport getPaidOwnedReport(Authentication authentication, Long reportId) {
        LifeReport report = getOwnedReport(authentication, reportId);
        if (!report.isPaid()) {
            throw new ProjectException(LifeTransitionErrorCode.PAYMENT_REQUIRED);
        }

        return report;
    }
}
