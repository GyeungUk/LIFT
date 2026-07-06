package com.lift.domain.lifetransition.enumtype;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 필요 서류 자동 조회(mock) 결과 상태.
 * 초기 MVP에서는 실제 공공 마이데이터/전자문서지갑 대신 발급 기관 기준으로 결정적으로 흉내 낸다.
 */
@Getter
@RequiredArgsConstructor
public enum DocumentFetchStatus {
    FETCHED("자동 조회 완료"),
    ACTION_REQUIRED("직접 준비 필요"),
    UNAVAILABLE("조회 불가");

    private final String displayName;
}
