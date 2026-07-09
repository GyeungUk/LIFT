package com.lift.domain.localnotice.service;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 지자체 RSS 지원사업/장려금 수집 파이프라인 설정.
 *
 * <p>{@code lift.public-data.gov24}({@link com.lift.domain.lifetransition.service.Gov24PublicServiceProperties})와
 * 동일한 구성 방식을 따른다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "lift.local-notice")
public class LocalNoticeProperties {

    /** true면 매 폴링마다 등록된 RSS 소스를 조회해 local_notice_item을 UPSERT하는 스케줄러가 동작한다. 기본 off. */
    private boolean syncEnabled = false;

    /** 동기화 실행 주기(cron). 기본 매시 정각. */
    private String syncCron = "0 0 * * * *";

    /**
     * 1차(코드) 제목 키워드 필터. 하나라도 제목에 포함되면 AI 판단 후보가 된다.
     *
     * <p>세 축으로 구성한다: (대상군) 생애주기별로 지원 대상이 되는 집단을 가리키는 말,
     * (지원형태) 실제 금전·서비스 지원을 뜻하는 말, (생애전환 상황) 이 앱 본연의 목적인
     * 퇴직·이직·실직 상황 자체를 가리키는 말. 파일럿(4개 지자체, 60건) 검증 중 제목만
     * 보고는 이 목록에 없어 놓친 실제 혜택 공고가 있었다(다문화가족 특별공급, 평생교육이용권,
     * 아동돌봄 기회소득, 경력보유여성 취업지원 등) — 이후 대상군에 다문화/여성/청소년/다자녀/
     * 신혼/1인가구/보훈, 지원형태에 이용권/기회소득/생계급여를 추가해 반영했다.
     *
     * <p>생애전환 상황 축은 처음 설계에 아예 빠져있던 것을 뒤늦게 추가했다 — 정작 이 앱의
     * 핵심 대상인 "퇴직/실직/이직" 자체를 가리키는 단어가 하나도 없었다. 새로 지어내지 않고,
     * {@link com.lift.domain.lifetransition.service.Gov24PublicBenefitService#buildKeywords}가
     * 정부24 매칭에 이미 쓰고 있어 검증된 단어만 가져왔다. 임금/소득세/체불/건강보험/국민연금/
     * 근로장려금처럼 지자체 게시판에서 무관한 기사(법적 분쟁, 세금 안내, 상시 제도 홍보 등)에
     * 걸릴 위험이 큰 단어는 제외했다.
     */
    private List<String> keywords = List.of(
            // 대상군(생애주기·계층)
            "청년", "청소년", "출산", "육아", "보육", "다자녀", "신혼", "노인", "어르신",
            "장애인", "한부모", "다문화", "여성", "저소득", "취약계층", "1인가구", "보훈", "국가유공자",
            // 지원형태
            "지원사업", "지원금", "장려금", "바우처", "이용권", "수당", "보조금", "기회소득", "생계급여",
            // 생애전환 상황(퇴직·이직·실직) — 이 앱 본연의 목적
            "퇴직", "정년", "실직", "이직", "구직", "실업", "재취업", "취업",
            "직업훈련", "고용보험", "국민취업지원", "내일배움"
    );

    /** 한 번의 동기화에서 소스당 가져올 최대 RSS 항목 수(전체 아카이브를 매번 훑지 않도록 상한). */
    private int maxFeedItemsPerSource = 50;

    /** 한 번의 동기화(1회 실행)에서 2차 AI 판단을 시도할 최대 건수. */
    private int maxJudgeBatchSize = 20;

    /**
     * AI 판단(2차 필터) 호출의 "누적(lifetime)" 상한. 매 실행마다 리셋되는
     * {@link #maxJudgeBatchSize}와 달리, 이미 판단을 마친 총 건수(DB의
     * {@code ai_judged_at IS NOT NULL} 행 수)를 기준으로 검사하므로 스케줄러가 몇 번을
     * 돌든 총 호출 수가 이 값을 넘지 않는다. 예산을 명시적으로 늘리려면 이 값을 올려야 한다.
     *
     * <p>gpt-5.4-mini 기준(2026-07 공개 요금: 입력 $0.75/백만 토큰, 출력 $4.50/백만 토큰) 호출 1건당
     * 프롬프트+요약+구조화 응답을 넉넉히(입력 2,000 토큰, 출력 800 토큰) 잡아도 약 $0.005 수준이라,
     * 기본값 200회는 최악의 경우에도 약 $1 내외로 추정된다(보수적으로 5배 여유를 둔 값).
     * 그래도 이 값만으로 지출을 "보장"하지는 않으므로, OpenAI 대시보드에서 별도의 하드 사용량
     * 한도(Usage limits)를 반드시 함께 설정할 것.
     */
    private int maxJudgeCallsTotal = 200;
}
