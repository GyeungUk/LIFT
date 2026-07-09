# 지자체 RSS 지원사업/장려금 수집 파이프라인 — 현황 정리

> Claude Code 등 다른 도구에서 이 작업을 이어가기 위한 핸드오프 문서. 작성 시점: 2026-07-09.

## 1. 목표

지자체 게시판(고시/공고)의 RSS 피드를 자동으로 수집해, **1차(코드) 키워드 필터 → 2차(AI) 맥락 판단**의
2단계 필터링으로 "실제 생애주기별 지원사업/장려금"만 걸러서 저장하는 파이프라인. AI에게 전체 원문을
다 읽히지 않고, 코드가 걸러낸 후보만 보내 토큰/비용을 아끼는 것이 핵심 설계 의도("하네스 엔지니어링").

파이프라인 4단계: **수집(RSS) → 1차 필터(코드, 제목 키워드) → 2차 판단(AI, 맥락) → 저장(참만)**.

기존 `com.lift.domain.lifetransition`(정부24 `Gov24BenefitCache`) 파이프라인과 동일한 패턴
(원문 캐시 + 변경감지 해시 + 단계별 상태 컬럼)을 재사용해서 새 도메인 `com.lift.domain.localnotice`로 구현했다.

## 2. 코드 파일 목록

| 파일 | 역할 |
|---|---|
| `src/main/java/com/lift/domain/localnotice/model/LocalNoticeSource.java` | RSS 소스(지자체 게시판) 레지스트리 엔티티 |
| `src/main/java/com/lift/domain/localnotice/model/LocalNoticeItem.java` | 수집 원문 + 1차/2차 판단 결과를 한 행에 담는 엔티티 (`gov24_benefit_cache`와 같은 패턴) |
| `src/main/java/com/lift/domain/localnotice/repository/LocalNoticeSourceRepository.java` | 소스 조회(`findByEnabledTrue`) |
| `src/main/java/com/lift/domain/localnotice/repository/LocalNoticeItemRepository.java` | UPSERT 조회, 판단 대기 후보 조회, 참 확정 목록 조회, 누적 AI 호출 수 카운트 |
| `src/main/java/com/lift/domain/localnotice/service/LocalNoticeProperties.java` | 설정(키워드 목록, 배치 크기, 예산 상한) — `lift.local-notice.*` |
| `src/main/java/com/lift/domain/localnotice/service/LocalNoticeFeedFetcher.java` | Rome 라이브러리로 RSS 파싱, HTML 태그 제거 |
| `src/main/java/com/lift/domain/localnotice/service/LocalNoticeTitleKeywordFilter.java` | 1차 필터(제목에 키워드 포함 여부, 공백/대소문자 무시) |
| `src/main/java/com/lift/domain/localnotice/service/LocalNoticeRelevanceJudgeService.java` | 2차 AI 판단. `OpenAiProperties`/Responses API 재사용(신규 키 불필요), `json_schema` strict 구조화 응답 |
| `src/main/java/com/lift/domain/localnotice/service/LocalNoticeSyncService.java` | 오케스트레이션(스케줄러 + 수동 실행), 예산 안전장치 로직 |
| `src/main/java/com/lift/domain/localnotice/controller/LocalNoticeAdminController.java` | 파일럿 검증용 수동 트리거 API (`/api/internal/local-notices/*`) |
| `src/main/resources/data.sql` (하단) | 로컬 H2용 파일럿 소스 4곳 시드 |
| `src/test/java/.../LocalNoticeTitleKeywordFilterTest.java`, `LocalNoticeItemTest.java` | 단위 테스트 |

`SecurityConfig`에 `/api/internal/local-notices/**`가 permitAll로 임시 등록되어 있음(별도 인가 체계 없어서, 운영 노출 시 반드시 정리 필요).

## 3. 현재 설정값 (2026-07-09 기준, 방금 확장함)

```java
// LocalNoticeProperties
keywords = [
  // 대상군
  "청년","청소년","출산","육아","보육","다자녀","신혼","노인","어르신",
  "장애인","한부모","다문화","여성","저소득","취약계층","1인가구","보훈","국가유공자",
  // 지원형태
  "지원사업","지원금","장려금","바우처","이용권","수당","보조금","기회소득","생계급여"
]
maxFeedItemsPerSource = 50   // 소스당 (기존 20에서 상향)
maxJudgeBatchSize = 20       // 1회 실행당 AI 호출 상한
maxJudgeCallsTotal = 200     // 누적(lifetime) AI 호출 상한 — 예산 안전장치, gpt-5.4-mini 기준 최악 약 $1
syncEnabled = false          // 운영 기본 off, LOCAL_NOTICE_SYNC_ENABLED로 켬
syncCron = "0 0 * * * *"     // 매시 정각
```

**⚠️ 처음 16개 키워드 → 27개로 확장했다.** 파일럿 60건을 사람이 직접 다 읽어본 결과, 아래처럼
제목에 원래 키워드가 없어서 1차 필터를 통과하지 못한 실제 혜택 공고가 발견됐다(false negative):

| 놓친 공고 | 빠진 단어 |
|---|---|
| 고양창릉 공공분양 특별공급 안내(**다문화가족**) | 다문화 |
| 2026년 서울시(노원구) 평생교육**이용권** 2차 신청 | 이용권 |
| 경기도 아동돌봄 **기회소득** 시행 안내 | 기회소득 |
| 경력보유**여성** 취업지원 디지털 편집디자인 과정 | 여성 |
| 지역사회서비스투자사업 이용자 모집(본문엔 노인/장애인 있으나 제목엔 없음) | — (제목만 검사하는 구조적 한계) |

## 4. DB 스키마

### 로컬(H2, `ddl-auto=update`)과 운영(Postgres) 공통 — JPA 엔티티 기준

**local_notice_source**: `id, region_sido, region_sigungu, org_name, board_name, feed_url, enabled, last_fetched_at, last_fetch_status, last_fetch_message`

**local_notice_item**: `id, source_id, region_sido, region_sigungu, guid, title, link, summary, published_at, content_hash, fetched_at, matched_keyword, ai_judged_at, ai_verdict, ai_category, ai_target_group_summary, ai_support_content_summary, ai_reason`

상태 해석: `matched_keyword IS NULL` → 1차 미통과. `matched_keyword IS NOT NULL AND ai_judged_at IS NULL` → 2차 판단 대기. `ai_judged_at IS NOT NULL`이면 `ai_verdict`로 참/거짓 확정(거짓도 재판단 안 하려고 보존).

### Supabase 파일럿 테이블 (기존 `gov24_benefit_cache`/`benefit_sources`와 완전히 별개, 검증용)

Supabase 프로젝트 `xqxwjannhcjueqlhfcww`(jaemin7's Project, ap-northeast-2)에 위 스키마와 동일한 컬럼으로
`public.local_notice_source`, `public.local_notice_item`을 만들어 실제 데이터를 넣어봤다.

**⚠️ RLS(Row Level Security)가 두 테이블 모두 비활성 상태** — anon key로 누구나 읽기/쓰기 가능한 상태다.
계속 쓸 거면 정책과 함께 RLS를 켜야 한다(운영 노출 전 필수).

## 5. Supabase에 실제로 넣은 파일럿 데이터 (2026-07-09, GPT 미사용 — Claude가 직접 2차 판단)

OpenAI 예산을 아끼기 위해, 2차 판단(원래 GPT가 할 일)을 **Claude가 직접 원문을 읽고 판단**해서 채웠다.
아래는 **구 설정(소스당 20건, 키워드 16개) 기준**으로 수집한 결과라 지금 설정과는 다르다 — 6번 "다음 작업" 참고.

| # | 지자체 | RSS 주소 | 수집 | 1차 후보 | 2차 참 |
|---|---|---|---|---|---|
| 1 | 서울 용산구청(고시/공고) | `https://www.yongsan.go.kr/portal/bbs/B0000095/rssService.do?menuNo=200233&viewType=CONTBODY` | 10 | 0 | 0 |
| 2 | 서울 노원구청(채용/고시/공고) | `https://www.nowon.kr/www/user/bbs/ND_selectRssList.do?q_bbsCode=1003&q_clCode=0` | 20 | 2 | 1 |
| 3 | 서울 광진구청(고시공고) | `https://www.gwangjin.go.kr/portal/bbs/B0000003/rssService.do?viewType=CONTBODY&bbsId=B03` | 10 | 0 | 0 |
| 4 | 경기 하남시청(열린시정-공지사항) | `https://www.hanam.go.kr/rssBbsNtt.do?bbsNo=30` | 20 | 8 | 6 |
| **합계** | | | **60** | **10** | **7** |

참으로 확정된 7건 예: "2026년 경기도 청년 복지포인트 모집"(반기 60만원, 최대 120만원 지역화폐), "경기청년 해외진출(유럽) 정착지원금(월 100만원, 최대 3개월)", "하남감일 공공지원 민간임대주택 청년분리형 특별공급" 등. 상세는 Supabase `local_notice_item WHERE ai_verdict = true`로 조회.

## 6. 확인된 제약/이슈

1. **키워드가 제목에만 적용됨** — 본문(summary)에 대상군이 있어도 제목에 없으면 1차에서 걸러짐 (지역사회서비스투자사업 사례).
2. **fetch가 순차·블로킹** — `LocalNoticeSyncService.syncNow()`가 소스를 for문으로 하나씩 처리, 소스당 최대 20초(connect 5s+read 15s) 타임아웃. 소스가 4개일 땐 문제없지만, 대한민국 전체(약 243개 기초/광역단체)로 확장하면 1회 동기화가 최소 8분~최대 80분까지 걸릴 수 있음(아직 병렬화/샤딩 없음).
3. **AI 예산은 소스 수와 무관하게 하드캡**(`maxJudgeCallsTotal=200`) — 전국 규모로 늘리면 초회 동기화에서 예산이 바로 소진되고 나머지는 다음 실행들로 밀림(의도된 안전장치).
4. **Supabase MCP로 한글 데이터 넣을 때 `\uXXXX` 수동 이스케이프 실수 주의** — 유니코드 코드포인트를 손으로 잘못 적으면 조용히 다른 글자로 깨짐(예: "노원구" → "닸원구"). 가능하면 리터럴 UTF-8 텍스트를 그대로 쓰고, 넣은 뒤 꼭 `select`로 재확인할 것.
5. **하남시 RSS의 `link`가 도메인이 아니라 내부 서버 IP(`27.101.118.12`)를 그대로 노출** — 원문 그대로이며 실제 브라우저 접속 시 하남시청으로 정상 연결됨.

## 7. 다음 작업 (미결)

- [ ] **파일럿 데이터 갱신**: 위 5번 표는 구설정(20건/16키워드) 기준. 사용자가 "지금 다시 가져와서 갱신"을 선택했으나 아직 미실행 — 새 설정(50건/27키워드)으로 4개 소스를 다시 fetch해서 Supabase `local_notice_item`을 갱신해야 함.
- [ ] Supabase 파일럿 테이블 RLS 정책 설정(또는 검증 끝나면 테이블 삭제).
- [ ] 전국 확장 시 fetch 병렬화(가상 스레드/`CompletableFuture`) 또는 소스 샤딩(매 실행마다 일부만) 검토.
- [ ] 실제 운영 시 2차 판단을 다시 OpenAI(`LocalNoticeRelevanceJudgeService`)로 되돌리고, `LOCAL_NOTICE_SYNC_ENABLED=true` + OpenAI 대시보드 하드 사용량 한도 설정.
