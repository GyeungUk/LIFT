package com.lift.domain.localnotice.service;

import com.lift.domain.localnotice.model.LocalNoticeItem;
import com.lift.domain.localnotice.model.LocalNoticeSource;
import com.lift.domain.localnotice.repository.LocalNoticeItemRepository;
import com.lift.domain.localnotice.repository.LocalNoticeSourceRepository;
import com.lift.domain.localnotice.service.LocalNoticeFeedFetcher.FetchResult;
import com.lift.domain.localnotice.service.LocalNoticeFeedFetcher.RawFeedItem;
import com.lift.domain.localnotice.service.LocalNoticeRelevanceJudgeService.JudgeResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지자체 RSS 지원사업/장려금 공고를 주기적으로 동기화한다.
 *
 * <p>동작 순서(각 단계는 {@code Gov24BenefitSyncService}와 같은 철학을 따른다):
 * <ol>
 *   <li>등록된 활성 RSS 소스를 전부 조회한다({@link LocalNoticeSourceRepository}).</li>
 *   <li>각 소스를 폴링해({@link LocalNoticeFeedFetcher}) 원문을 (소스,guid) 기준 UPSERT한다.</li>
 *   <li>1차 필터(제목 키워드, {@link LocalNoticeTitleKeywordFilter})로 후보 여부를 코드가 정한다.</li>
 *   <li>아직 판단 안 된(ai_judged_at IS NULL) 후보만 AI로 참/거짓을 판단한다({@link LocalNoticeRelevanceJudgeService}).</li>
 * </ol>
 *
 * <p>AI 호출은 "제목 키워드가 걸린 신규/변경 건"에만 일어나므로, 전체 RSS 수집량과 무관하게
 * 실제 후보 수에만 비용이 묶인다.
 *
 * <p>스케줄러는 {@code lift.local-notice.sync-enabled=true}일 때만 빈으로 등록된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "lift.local-notice", name = "sync-enabled", havingValue = "true")
public class LocalNoticeSyncService {

    private final LocalNoticeSourceRepository sourceRepository;
    private final LocalNoticeItemRepository itemRepository;
    private final LocalNoticeFeedFetcher feedFetcher;
    private final LocalNoticeTitleKeywordFilter keywordFilter;
    private final LocalNoticeRelevanceJudgeService judgeService;
    private final LocalNoticeProperties properties;

    /** 등록된 소스별 polling(cron). 기본 매시 정각. */
    @Scheduled(cron = "${lift.local-notice.sync-cron:0 0 * * * *}")
    public void scheduledSync() {
        log.info("지자체 RSS 공고 동기화 시작(스케줄).");
        SyncResult result = syncNow();
        log.info("지자체 RSS 공고 동기화 완료(스케줄): {}", result);
    }

    /**
     * 동기화를 즉시 실행한다(수동 트리거/검증용). 결과 요약을 반환한다.
     */
    @Transactional
    public SyncResult syncNow() {
        List<LocalNoticeSource> sources = sourceRepository.findByEnabledTrue();

        int fetched = 0;
        int inserted = 0;
        int updated = 0;
        int unchanged = 0;
        int candidates = 0;

        for (LocalNoticeSource source : sources) {
            FetchResult result = feedFetcher.fetch(source.getFeedUrl(), properties.getMaxFeedItemsPerSource());
            if (!result.success()) {
                source.markFetchFailure(result.errorMessage());
                log.warn("RSS 소스 조회 실패. source={}, url={}, error={}",
                        source.getOrgName(), source.getFeedUrl(), result.errorMessage());
                continue;
            }
            source.markFetchSuccess();

            for (RawFeedItem raw : result.items()) {
                fetched++;
                String matchedKeyword = keywordFilter.matchKeyword(raw.title(), properties.getKeywords()).orElse(null);
                if (matchedKeyword != null) {
                    candidates++;
                }
                String hash = contentHash(raw);

                Optional<LocalNoticeItem> existing = itemRepository.findBySourceIdAndGuid(source.getId(), raw.guid());
                if (existing.isEmpty()) {
                    itemRepository.save(LocalNoticeItem.create(
                            source, raw.guid(), raw.title(), raw.link(), raw.summary(), raw.publishedAt(),
                            hash, matchedKeyword
                    ));
                    inserted++;
                } else {
                    LocalNoticeItem item = existing.get();
                    if (hash.equals(item.getContentHash())) {
                        unchanged++;
                    } else {
                        item.updateRawContent(raw.title(), raw.link(), raw.summary(), raw.publishedAt(), hash, matchedKeyword);
                        updated++;
                    }
                }
            }
        }

        JudgeSummary judge = runJudging();
        SyncResult result = new SyncResult(sources.size(), fetched, inserted, updated, unchanged, candidates,
                judge.judged(), judge.verifiedTrue(), judge.budgetRemaining());
        log.info("동기화 결과: 소스 {}곳, 항목 {}건(신규 {}/변경 {}/유지 {}), 후보 {}건, AI판단 {}건(참 {}건), "
                        + "AI 호출 누적 예산 잔여 {}건",
                sources.size(), fetched, inserted, updated, unchanged, candidates,
                judge.judged(), judge.verifiedTrue(), judge.budgetRemaining());
        return result;
    }

    /**
     * 1차 필터를 통과했지만 아직 AI 판단이 안 된 행에 대해 참/거짓 판단을 수행한다.
     *
     * <p>두 종류의 상한을 함께 적용한다.
     * <ul>
     *   <li>{@code maxJudgeBatchSize} — 이번 1회 실행에서 시도할 최대 건수.</li>
     *   <li>{@code maxJudgeCallsTotal} — 지금까지(전체 기간) 누적 호출 건수 상한. 스케줄러가
     *       몇 번을 돌든 이 값을 넘는 순간부터는 AI를 아예 호출하지 않는다(비용 안전장치).</li>
     * </ul>
     * 판단 자체가 시스템적으로 실패(크레딧 부족 등)하면 첫 실패에서 중단해 불필요한 호출을 막는다.
     */
    private JudgeSummary runJudging() {
        if (!judgeService.isEnabled()) {
            log.info("OpenAI 비활성 상태 — 2차 AI 판단을 건너뛴다(1차 후보만 대기 상태로 남음).");
            return new JudgeSummary(0, 0, properties.getMaxJudgeCallsTotal());
        }

        long alreadyCalled = itemRepository.countByAiJudgedAtIsNotNull();
        long totalBudget = Math.max(0, properties.getMaxJudgeCallsTotal());
        long remainingBudget = totalBudget - alreadyCalled;
        if (remainingBudget <= 0) {
            log.warn("AI 호출 누적 예산({}건)을 이미 사용했다(누적 {}건) — 2차 AI 판단을 건너뛴다. "
                            + "예산을 늘리려면 lift.local-notice.max-judge-calls-total을 올려라.",
                    totalBudget, alreadyCalled);
            return new JudgeSummary(0, 0, 0);
        }

        List<LocalNoticeItem> pending = itemRepository.findByMatchedKeywordIsNotNullAndAiJudgedAtIsNull();
        int limit = (int) Math.min(Math.max(1, properties.getMaxJudgeBatchSize()), remainingBudget);

        int judged = 0;
        int verifiedTrue = 0;
        int attempted = 0;
        for (LocalNoticeItem item : pending) {
            if (attempted >= limit) {
                break;
            }
            attempted++;
            Optional<JudgeResult> verdict = judgeService.judge(item.getTitle(), item.getSummary());
            if (verdict.isEmpty()) {
                // API 오류/크레딧 부족으로 판단 — 이 행은 미판단으로 남겨 다음 동기화에서 재시도.
                // 시스템적 장애일 가능성이 높으므로 이번 실행에서는 추가 호출을 멈춘다.
                break;
            }
            JudgeResult verdictResult = verdict.get();
            item.applyAiVerdict(
                    verdictResult.isLifecycleSupport(),
                    verdictResult.category(),
                    verdictResult.targetGroupSummary(),
                    verdictResult.supportContentSummary(),
                    verdictResult.reason()
            );
            judged++;
            if (verdictResult.isLifecycleSupport()) {
                verifiedTrue++;
            }
        }
        return new JudgeSummary(judged, verifiedTrue, remainingBudget - judged);
    }

    /** 원문(guid|제목|링크|요약|게시일)의 MD5. */
    private String contentHash(RawFeedItem raw) {
        String joined = raw.guid() + "|" + nullToBlank(raw.title()) + "|" + nullToBlank(raw.link())
                + "|" + nullToBlank(raw.summary()) + "|" + (raw.publishedAt() == null ? "" : raw.publishedAt().toString());
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(joined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    /** 동기화 결과 요약. {@code judgeBudgetRemaining}은 누적 AI 호출 예산의 남은 건수다. */
    public record SyncResult(
            int sourceCount,
            int fetched,
            int inserted,
            int updated,
            int unchanged,
            int candidates,
            int judged,
            int verifiedTrue,
            long judgeBudgetRemaining
    ) {
    }

    private record JudgeSummary(int judged, int verifiedTrue, long budgetRemaining) {
    }
}
