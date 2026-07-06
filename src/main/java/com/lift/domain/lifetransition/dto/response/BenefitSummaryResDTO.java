package com.lift.domain.lifetransition.dto.response;

/**
 * 리포트 전체의 예상 금액 요약. 리포트 상단/‌PDF 헤드라인에 사용한다.
 *
 * @param totalReceiveAmount 일시로 받을 수 있는 금액 합계(실업급여 총액 + 퇴직금 등)
 * @param totalMonthlySaving 매달 아낄 수 있는 금액 합계(국민연금 납부예외 등)
 * @param receiveItemCount   금액이 산정된 '받는' 항목 수
 * @param hasVariable        금액 변동(세금 등) 항목 포함 여부
 * @param estimated          입력값이 있어 금액이 하나라도 계산되었는지
 * @param basisNote          산정 기준/면책 문구
 */
public record BenefitSummaryResDTO(
        long totalReceiveAmount,
        long totalMonthlySaving,
        int receiveItemCount,
        boolean hasVariable,
        boolean estimated,
        String basisNote
) {
}
