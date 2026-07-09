package com.lift.domain.localnotice.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 1차 필터(순수 코드). RSS로 들어온 공고의 "제목"만 보고 키워드 후보 여부를 가볍게 판별한다.
 *
 * <p>DB/AI를 전혀 호출하지 않으므로 비용이 0이다. 여기서 걸러진(키워드가 하나도 없는) 공고는
 * 2차 AI 판단으로 넘어가지 않는다 — 토큰 사용량이 "제목에 키워드가 하나라도 걸린 신규/변경
 * 공고 수"에만 비례하도록 만드는 핵심 장치다.
 */
@Component
public class LocalNoticeTitleKeywordFilter {

    /**
     * 제목에 포함된 첫 번째 매칭 키워드를 반환한다. 공백/대소문자 차이로 놓치는 것을 막기 위해
     * 둘 다 정규화한 뒤 부분 문자열로 비교한다(형태소 분석 등 정교한 매칭은 하지 않는다 —
     * 1차 필터는 "재현율(놓치지 않기)"을 우선하고, 정밀 판단은 2차 AI가 담당한다).
     */
    public Optional<String> matchKeyword(String title, List<String> keywords) {
        if (!StringUtils.hasText(title) || keywords == null || keywords.isEmpty()) {
            return Optional.empty();
        }
        String normalizedTitle = normalize(title);
        return keywords.stream()
                .filter(StringUtils::hasText)
                .filter(keyword -> normalizedTitle.contains(normalize(keyword)))
                .findFirst();
    }

    private String normalize(String value) {
        return value.replaceAll("\\s+", "").toLowerCase(Locale.KOREAN);
    }
}
