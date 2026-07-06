package com.bodeum.domain.lifetransition.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * PDF 저장 시점에만 쓰는 임시 금액 계산 입력값.
 * 값은 저장하지 않고, 응답 리포트의 예상 금액 재계산에만 사용한다.
 */
public record ReportPdfEstimateReqDTO(
        @Min(value = 1, message = "월 평균임금은 1원 이상이어야 합니다.")
        @Max(value = 100_000_000, message = "월 평균임금을 확인해주세요.")
        Integer monthlyAverageWage
) {
}
