package com.lift.domain.localnotice.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * {@link LocalNoticeItem}의 상태 전이 단위 테스트.
 * "원문 변경 시 AI 재판단 대상으로 되돌아간다"는 gov24_benefit_cache와 동일한 계약을 검증한다.
 */
class LocalNoticeItemTest {

    private LocalNoticeSource source() {
        return LocalNoticeSource.create("서울특별시", "용산구", "용산구청", "고시/공고",
                "https://www.yongsan.go.kr/portal/bbs/B0000095/rssService.do", true);
    }

    @Test
    void create는_매칭된_키워드와_지역정보를_그대로_반영한다() {
        LocalNoticeItem item = LocalNoticeItem.create(
                source(), "guid-1", "청년 월세 지원사업 공고", "https://example.com/1",
                "만 19~34세 청년 대상 월세 지원", Instant.now(), "hash-1", "청년"
        );

        assertThat(item.getRegionSido()).isEqualTo("서울특별시");
        assertThat(item.getRegionSigungu()).isEqualTo("용산구");
        assertThat(item.getMatchedKeyword()).isEqualTo("청년");
        assertThat(item.getAiJudgedAt()).isNull();
        assertThat(item.getAiVerdict()).isNull();
    }

    @Test
    void applyAiVerdict는_판단_시각과_결과를_기록한다() {
        LocalNoticeItem item = LocalNoticeItem.create(
                source(), "guid-1", "청년 월세 지원사업 공고", "https://example.com/1",
                "만 19~34세 청년 대상 월세 지원", Instant.now(), "hash-1", "청년"
        );

        item.applyAiVerdict(true, "청년", "만 19~34세 청년", "월세 일부 지원", "제목과 요약에 신청 대상/지원 내용이 명확함");

        assertThat(item.getAiJudgedAt()).isNotNull();
        assertThat(item.getAiVerdict()).isTrue();
        assertThat(item.getAiCategory()).isEqualTo("청년");
        assertThat(item.getAiReason()).isNotBlank();
    }

    @Test
    void updateRawContent은_원문이_바뀌면_AI_판단결과를_초기화해_재판단_대상으로_되돌린다() {
        LocalNoticeItem item = LocalNoticeItem.create(
                source(), "guid-1", "청년 월세 지원사업 공고", "https://example.com/1",
                "만 19~34세 청년 대상 월세 지원", Instant.now(), "hash-1", "청년"
        );
        item.applyAiVerdict(true, "청년", "만 19~34세 청년", "월세 일부 지원", "확정");
        assertThat(item.getAiJudgedAt()).isNotNull();

        item.updateRawContent("청년 월세 지원사업 공고(마감 연장)", "https://example.com/1",
                "만 19~34세 청년 대상 월세 지원, 마감 12/31로 연장", Instant.now(), "hash-2", "청년");

        assertThat(item.getContentHash()).isEqualTo("hash-2");
        assertThat(item.getAiJudgedAt()).isNull();
        assertThat(item.getAiVerdict()).isNull();
        assertThat(item.getAiReason()).isNull();
    }
}
