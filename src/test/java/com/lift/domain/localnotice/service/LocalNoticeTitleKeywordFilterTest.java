package com.lift.domain.localnotice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 1차(코드) 키워드 필터 단위 테스트. Spring 컨텍스트 없이 순수 로직만 검증한다.
 */
class LocalNoticeTitleKeywordFilterTest {

    private final LocalNoticeTitleKeywordFilter filter = new LocalNoticeTitleKeywordFilter();

    private static final List<String> KEYWORDS = List.of("청년", "출산", "장려금", "바우처");

    @Test
    void 제목에_키워드가_포함되면_해당_키워드를_반환한다() {
        Optional<String> matched = filter.matchKeyword("2026년 청년 월세 지원사업 모집 공고", KEYWORDS);

        assertThat(matched).contains("청년");
    }

    @Test
    void 공백이_섞여도_매칭한다() {
        Optional<String> matched = filter.matchKeyword("출  산 장려금 신청 안내", KEYWORDS);

        assertThat(matched).contains("출산");
    }

    @Test
    void 대소문자와_무관하게_매칭한다() {
        Optional<String> matched = filter.matchKeyword("Youth Voucher 지원사업 공고", List.of("voucher"));

        assertThat(matched).contains("voucher");
    }

    @Test
    void 키워드가_없으면_빈_값을_반환한다() {
        Optional<String> matched = filter.matchKeyword("2026년도 도로 보수공사 입찰 공고", KEYWORDS);

        assertThat(matched).isEmpty();
    }

    @Test
    void 제목이_비어있으면_빈_값을_반환한다() {
        assertThat(filter.matchKeyword(null, KEYWORDS)).isEmpty();
        assertThat(filter.matchKeyword("", KEYWORDS)).isEmpty();
        assertThat(filter.matchKeyword("공고", List.of())).isEmpty();
    }
}
