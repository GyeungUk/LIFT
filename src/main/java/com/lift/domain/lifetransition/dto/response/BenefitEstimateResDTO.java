package com.lift.domain.lifetransition.dto.response;

import com.lift.domain.lifetransition.enumtype.BenefitEstimateKind;

/**
 * 개별 절차의 예상 금액. 모든 금액은 예상치이며 실제 금액은 기관 판단에 따라 달라진다.
 *
 * @param kind        금액 성격(받음/절감/변동/미산정)
 * @param amount      원 단위 금액. VARIABLE / NOT_ESTIMATED 인 경우 null.
 * @param amountLabel 화면 표시용 요약 라벨. 예) "약 1,200만원", "월 약 27만원"
 * @param headline    한 줄 설명. 예) "실업급여로 약 1,200만원을 받을 수 있어요"
 * @param detail      계산 근거 요약. 예) "1일 66,000원 × 180일"
 */
public record BenefitEstimateResDTO(
        BenefitEstimateKind kind,
        Long amount,
        String amountLabel,
        String headline,
        String detail
) {
}
