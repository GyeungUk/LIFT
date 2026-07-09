package com.lift.domain.localnotice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * RSS로 수집한 지자체 공고 원문 캐시 1건.
 *
 * <p>{@code gov24_benefit_cache}와 동일한 "원문 + 판정 결과를 한 테이블에 보관" 패턴을 쓴다.
 * 모든 수집 항목이 이 테이블에 저장되며(전체 캐시), 단계별 컬럼으로 진행 상태를 구분한다.
 *
 * <ul>
 *   <li>{@code matchedKeyword == null} → 1차(코드) 키워드 필터를 통과하지 못한 공고. AI를 호출하지 않는다.</li>
 *   <li>{@code matchedKeyword != null && aiJudgedAt == null} → 2차 AI 판단 대기(재시도 대상).</li>
 *   <li>{@code aiJudgedAt != null && aiVerdict == true} → 실제 생애주기 지원사업/장려금으로 확정된 공고.</li>
 *   <li>{@code aiJudgedAt != null && aiVerdict == false} → AI가 아니라고 판단한 공고(재판단하지 않도록 보존).</li>
 * </ul>
 */
@Entity
@Getter
@Table(name = "local_notice_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalNoticeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_id")
    private Long sourceId;

    /** 수집 시점에 소스에서 복사해 둔 지역 정보(다운스트림 지역 매칭용). */
    @Column(name = "region_sido")
    private String regionSido;

    @Column(name = "region_sigungu")
    private String regionSigungu;

    /** RSS 항목의 guid(또는 guid가 없으면 link). 소스 내에서 고유해야 UPSERT 키로 쓸 수 있다. */
    @Column(name = "guid")
    private String guid;

    @Column(name = "title")
    private String title;

    @Column(name = "link")
    private String link;

    /** RSS description(HTML 태그 제거된 요약 텍스트). AI 2차 판단의 입력이 된다. */
    @Column(name = "summary", length = 4000)
    private String summary;

    @Column(name = "published_at")
    private Instant publishedAt;

    /** 원문(제목+링크+요약+게시일) 해시. 동기화 시 변경 감지에 쓴다. */
    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "fetched_at")
    private Instant fetchedAt;

    /** 1차 필터에서 매칭된 키워드. null이면 후보가 아니다(AI 호출 대상 아님). */
    @Column(name = "matched_keyword")
    private String matchedKeyword;

    /** 2차 AI 판단을 마친(또는 마치려 시도한) 시각. null이면 아직 미판단(재시도 대상)이다. */
    @Column(name = "ai_judged_at")
    private Instant aiJudgedAt;

    /** true = 실제 생애주기 지원사업/장려금으로 확정. false = AI가 아니라고 판단. */
    @Column(name = "ai_verdict")
    private Boolean aiVerdict;

    /** 청년/출산/육아/노인/장애인/기타 등. AI가 원문에서 명확히 판단 가능한 경우만 채운다. */
    @Column(name = "ai_category")
    private String aiCategory;

    @Column(name = "ai_target_group_summary", length = 1000)
    private String aiTargetGroupSummary;

    @Column(name = "ai_support_content_summary", length = 1000)
    private String aiSupportContentSummary;

    /** AI 판단 근거(운영 중 QA용). 참/거짓 여부와 무관하게 항상 남긴다. */
    @Column(name = "ai_reason", length = 1000)
    private String aiReason;

    public static LocalNoticeItem create(
            LocalNoticeSource source,
            String guid,
            String title,
            String link,
            String summary,
            Instant publishedAt,
            String contentHash,
            String matchedKeyword
    ) {
        LocalNoticeItem item = new LocalNoticeItem();
        item.sourceId = source.getId();
        item.regionSido = source.getRegionSido();
        item.regionSigungu = source.getRegionSigungu();
        item.guid = guid;
        item.title = title;
        item.link = link;
        item.summary = summary;
        item.publishedAt = publishedAt;
        item.contentHash = contentHash;
        item.fetchedAt = Instant.now();
        item.matchedKeyword = matchedKeyword;
        return item;
    }

    /**
     * 원문이 바뀐 기존 공고를 갱신한다. 원문이 달라졌으므로 AI 재판단 대상이 되도록
     * {@code aiJudgedAt}을 null로 되돌린다(다음 판단 배치의 재시도 대상).
     */
    public void updateRawContent(
            String title,
            String link,
            String summary,
            Instant publishedAt,
            String contentHash,
            String matchedKeyword
    ) {
        this.title = title;
        this.link = link;
        this.summary = summary;
        this.publishedAt = publishedAt;
        this.contentHash = contentHash;
        this.fetchedAt = Instant.now();
        this.matchedKeyword = matchedKeyword;
        this.aiJudgedAt = null;
        this.aiVerdict = null;
        this.aiCategory = null;
        this.aiTargetGroupSummary = null;
        this.aiSupportContentSummary = null;
        this.aiReason = null;
    }

    /** AI가 참/거짓을 판단해 응답한 경우 반영한다(참/거짓 모두 이 메서드로 기록됨). */
    public void applyAiVerdict(
            boolean verdict,
            String category,
            String targetGroupSummary,
            String supportContentSummary,
            String reason
    ) {
        this.aiJudgedAt = Instant.now();
        this.aiVerdict = verdict;
        this.aiCategory = category;
        this.aiTargetGroupSummary = targetGroupSummary;
        this.aiSupportContentSummary = supportContentSummary;
        this.aiReason = reason;
    }
}
