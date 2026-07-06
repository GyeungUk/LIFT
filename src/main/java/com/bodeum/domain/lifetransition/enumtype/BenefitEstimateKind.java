package com.bodeum.domain.lifetransition.enumtype;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 예상 금액의 성격. 프론트가 "받는 돈 / 아끼는 돈 / 변동" 을 구분해 표시하는 데 사용한다.
 */
@Getter
@RequiredArgsConstructor
public enum BenefitEstimateKind {

    /** 신청하면 일시(또는 총액)로 받는 금액. 예) 실업급여 총 수급액, 퇴직금. */
    RECEIVE("받을 수 있어요"),

    /** 매달 아끼거나 유예되는 금액. 예) 국민연금 납부예외, 건강보험 임의계속. */
    SAVE_MONTHLY("매달 아낄 수 있어요"),

    /** 금액이 개인 상황에 따라 달라 확정할 수 없는 경우. 예) 세금 정산 환급. */
    VARIABLE("금액은 상황에 따라 달라요"),

    /** 계산에 필요한 입력이 없어 산정하지 못한 경우. */
    NOT_ESTIMATED("입력하면 계산해 드려요");

    private final String label;
}
