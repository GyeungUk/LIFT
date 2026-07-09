package com.lift.domain.lifetransition.enumtype;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 결제 리포트 플랜. 화면의 가격 선택과 서버의 기능 권한을 같은 값으로 묶는다.
 */
@Getter
@RequiredArgsConstructor
public enum ReportPlanType {

    // pdfAvailable: PDF 저장 제공 여부. aiQuestionLimit: "하루" AI 질문 허용 횟수.
    // 기본(6,900원)은 리포트 열람만, 확장(13,900원)은 PDF 저장 + 하루 10회 AI 질문을 제공한다.
    BASIC(6_900, 0, false, "기본 리포트"),
    PLUS(13_900, 10, true, "확장 리포트");

    private final int price;
    private final int aiQuestionLimit;
    private final boolean pdfAvailable;
    private final String displayName;

    public static ReportPlanType findByPrice(Integer price) {
        if (price == null) {
            return null;
        }

        for (ReportPlanType plan : values()) {
            if (plan.price == price) {
                return plan;
            }
        }
        return null;
    }
}
