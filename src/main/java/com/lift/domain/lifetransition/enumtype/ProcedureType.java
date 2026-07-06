package com.lift.domain.lifetransition.enumtype;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 생애 전환 상황에서 놓치기 쉬운 행정 절차 유형.
 * displayName / officialUrl은 초기 MVP 기본값이며, 추후 rules.json 또는 DB로 분리할 수 있다.
 */
@Getter
@RequiredArgsConstructor
public enum ProcedureType {
    UNEMPLOYMENT_BENEFIT("실업급여(구직급여)", "https://www.work24.go.kr"),
    HEALTH_INSURANCE_CONTINUATION("건강보험 임의계속가입", "https://www.nhis.or.kr"),
    NATIONAL_PENSION_EXCEPTION("국민연금 납부예외", "https://www.nps.or.kr"),
    TAX_CHECK("세금 정산 체크(연말정산/종합소득세)", "https://www.hometax.go.kr"),
    SEVERANCE_PAY("퇴직금 정산 확인", "https://www.moel.go.kr");

    private final String displayName;
    private final String defaultOfficialUrl;
}
