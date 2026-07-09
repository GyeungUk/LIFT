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
 * 지자체 게시판 RSS 피드 레지스트리.
 *
 * <p>지자체마다 게시판 시스템(전자정부 표준프레임워크, 자체 CMS 등)이 달라 RSS 주소 형태가
 * 제각각이므로, 코드/설정이 아니라 이 테이블로 피드를 관리한다. 지자체 추가·URL 변경·장애
 * 지자체 비활성화를 배포 없이 운영할 수 있게 하기 위함이다.
 *
 * <p>{@code regionSido}/{@code regionSigungu}는 이 피드가 어느 지역 공고인지를 나타내며,
 * 수집된 각 {@link LocalNoticeItem}에 그대로 복사돼 지역 매칭에 쓰인다(정부24 캐시가
 * 소관기관명 문자열에서 지역을 매번 파싱하는 것과 달리, 수집 시점에 이미 알고 있는 값이므로
 * 저장 시 바로 채운다).
 */
@Entity
@Getter
@Table(name = "local_notice_source")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalNoticeSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_sido")
    private String regionSido;

    @Column(name = "region_sigungu")
    private String regionSigungu;

    /** 예: "용산구청". */
    @Column(name = "org_name")
    private String orgName;

    /** 예: "고시공고", "새소식". */
    @Column(name = "board_name")
    private String boardName;

    @Column(name = "feed_url")
    private String feedUrl;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "last_fetched_at")
    private Instant lastFetchedAt;

    /** SUCCESS / FAILED. 운영 중 장애 지자체를 빠르게 식별하기 위한 값이다. */
    @Column(name = "last_fetch_status")
    private String lastFetchStatus;

    @Column(name = "last_fetch_message")
    private String lastFetchMessage;

    public static LocalNoticeSource create(
            String regionSido,
            String regionSigungu,
            String orgName,
            String boardName,
            String feedUrl,
            boolean enabled
    ) {
        LocalNoticeSource source = new LocalNoticeSource();
        source.regionSido = regionSido;
        source.regionSigungu = regionSigungu;
        source.orgName = orgName;
        source.boardName = boardName;
        source.feedUrl = feedUrl;
        source.enabled = enabled;
        return source;
    }

    public void markFetchSuccess() {
        this.lastFetchedAt = Instant.now();
        this.lastFetchStatus = "SUCCESS";
        this.lastFetchMessage = null;
    }

    public void markFetchFailure(String message) {
        this.lastFetchedAt = Instant.now();
        this.lastFetchStatus = "FAILED";
        this.lastFetchMessage = message;
    }
}
