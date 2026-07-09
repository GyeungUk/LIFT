package com.lift.domain.localnotice.controller;

import com.lift.domain.localnotice.model.LocalNoticeItem;
import com.lift.domain.localnotice.repository.LocalNoticeItemRepository;
import com.lift.domain.localnotice.service.LocalNoticeSyncService;
import com.lift.domain.localnotice.service.LocalNoticeSyncService.SyncResult;
import com.lift.global.apiPayload.ApiResponse;
import com.lift.global.apiPayload.code.GeneralSuccessCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지자체 RSS 공고 수집 파이프라인을 수동으로 실행/조회하기 위한 임시(파일럿 검증용) 엔드포인트.
 *
 * <p>스케줄러(기본 매시 정각)를 기다리지 않고 파이프라인 동작을 즉시 확인하려는 목적이다.
 * {@code lift.local-notice.sync-enabled=true}일 때만 빈으로 등록되므로(운영 기본값은 off),
 * 이 플래그를 켜지 않는 한 존재하지 않는다. 다만 켜져 있는 동안에는 {@code SecurityConfig}의
 * permitAll 목록에 포함돼 인증 없이 호출 가능하다 — 이 프로젝트에 아직 별도 관리자 권한
 * 체계가 없기 때문에 택한 파일럿 단계의 임시 조치이며, 운영에 노출할 경우 반드시 인증/인가를
 * 추가하거나 이 컨트롤러를 제거해야 한다.
 */
@RestController
@RequestMapping("/api/internal/local-notices")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "lift.local-notice", name = "sync-enabled", havingValue = "true")
public class LocalNoticeAdminController {

    private final LocalNoticeSyncService syncService;
    private final LocalNoticeItemRepository itemRepository;

    /** 지금 바로 RSS 수집 → 1차 키워드 필터 → 2차 AI 판단을 1회 실행하고 결과 요약을 돌려준다. */
    @PostMapping("/sync")
    public ApiResponse<SyncResult> syncNow() {
        return ApiResponse.of(GeneralSuccessCode.OK, syncService.syncNow());
    }

    /** AI가 실제 생애주기 지원사업/장려금으로 확정한(ai_verdict=true) 공고 목록. */
    @GetMapping("/verified")
    public ApiResponse<List<LocalNoticeItem>> verified() {
        return ApiResponse.of(GeneralSuccessCode.OK, itemRepository.findByAiVerdictTrueOrderByPublishedAtDesc());
    }

    /** 수집된 전체 캐시(필터/판단 상태 확인용, 디버깅 목적). */
    @GetMapping("/items")
    public ApiResponse<List<LocalNoticeItem>> items() {
        return ApiResponse.of(GeneralSuccessCode.OK, itemRepository.findAll());
    }
}
