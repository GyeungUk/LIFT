package com.lift.domain.lifetransition.service;

import com.lift.domain.lifetransition.model.LifeReport;

/**
 * 리포트 기반 AI 설명/질의응답 서비스.
 *
 * <p>AI는 자격 판단을 하지 않는다. 룰 엔진이 계산한 리포트를 근거로 사용자 질문을 쉬운 말로
 * 설명하는 역할만 한다. 현재는 mock 구현({@link MockLifeReportAiService})을 사용하고,
 * 추후 OpenAI 등 실제 LLM 연동체로 교체할 수 있도록 인터페이스로 분리한다.
 */
public interface LifeReportAiService {

    /**
     * 리포트 맥락과 사용자 질문을 받아 AI 응답 텍스트를 생성한다.
     *
     * @param report       질문 대상 리포트(요약/항목 등 근거)
     * @param userQuestion 사용자의 질문
     * @return AI 응답 본문
     */
    String generateAnswer(LifeReport report, String userQuestion);
}
