package com.lift.domain.localnotice.repository;

import com.lift.domain.localnotice.model.LocalNoticeItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalNoticeItemRepository extends JpaRepository<LocalNoticeItem, Long> {

    /** 동기화 시 (소스, guid) 기준으로 기존 행을 찾아 UPSERT 판단에 쓴다. */
    Optional<LocalNoticeItem> findBySourceIdAndGuid(Long sourceId, String guid);

    /** 1차 필터를 통과했지만 아직 AI 판단이 안 된(재시도 대상) 후보 목록. */
    List<LocalNoticeItem> findByMatchedKeywordIsNotNullAndAiJudgedAtIsNull();

    /** AI가 실제 생애주기 지원사업/장려금으로 확정한 공고만(다운스트림 매칭/노출 대상). */
    List<LocalNoticeItem> findByAiVerdictTrueOrderByPublishedAtDesc();

    /** 지금까지 AI 호출을 시도해 응답을 받은 누적 건수. 비용 상한(예산) 집행에 쓴다. */
    long countByAiJudgedAtIsNotNull();
}
