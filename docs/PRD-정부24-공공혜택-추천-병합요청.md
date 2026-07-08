# [PRD] 정부24 공공혜택 DB 캐싱 & 사용자 맞춤형 추천 기능 — `main` 병합 요청서

> 작성자: (기능 개발자)
> 대상: `main` 브랜치 관리자 / 리뷰어
> 브랜치: `feature/gov24-benefit-cache`
> 목적: 아래 기능을 `main`에 **충돌을 최소화하며** 병합하기 위한 통합 가이드
> ⚠️ 주의: 본 문서는 `main`의 최신 상태를 알지 못한 채 작성됐다. 실제 겹침 여부는 병합 시점에 §7.1 셀프체크로 확인할 것.

---

## 0. TL;DR (요약)

- **무엇을 추가했나**: 정부24(보조금24) 공공서비스 혜택 데이터를 **DB에 캐싱**해 두고, 사용자가 입력한 진단 정보(나이·근속연수·고용보험 가입기간·이직사유)를 기준으로 **조건에 맞는 혜택만 매칭·추천**하는 기능.
- **어디에 있나**: `feature/gov24-benefit-cache` 브랜치의 단일 기능 커밋(`정부24 공공혜택 DB 저장 및 사용자 맞춤형 추천 기능 추가`).
- **충돌 위험(전제)**: ⚠️ **본 문서는 `main`의 현재 상태를 정확히 알지 못한 상태에서 작성됐다.** `main`은 다른 개발자가 계속 발전시키고 있어, 아래 스냅샷 비교 이후 더 나아갔을 수 있다. 따라서 "충돌 0"이라고 단정하지 않고, **main 상태와 무관하게 성립하는 구조적 안전성**과, **병합 시점에 리뷰어가 직접 확인하는 방법**(§7)을 함께 제공한다.
- **구조적으로 안전한 부분 (main이 얼마나 나갔든 무관)**: 이 기능의 백엔드 변경은 (a) **신규 파일 8개**(같은 경로를 main이 새로 만들지 않는 한 충돌 불가) + (b) 기존 파일에 **필드/메서드/엔드포인트 추가(가법)** 로만 구성. 핵심 로직은 이 기능이 단독 소유하는 `Gov24PublicBenefitService` 한 곳에 집중되어 있다.
- **직접 확인이 필요한 부분 (main이 같은 곳을 손댔을 수 있음)**: 주로 **프론트엔드 UI 파일**(`globals.css`, `report/[id]/page.tsx`, `demo.ts`, `types.ts`, `api.ts`, `assessment/new/page.tsx`). 이 기능의 변경은 전부 "새 블록 추가" 성격이라, 충돌이 나더라도 **양쪽 유지(Accept Both)**로 해결된다.
- **참고용 스냅샷 비교**: 작성 시점에 확인 가능했던 `main`(LIFE-SFHIFT `a8319dc`) 기준으로는, 이 기능이 바꾼 22개 파일과 main의 변경 파일 4개(`Dockerfile`, `OpenAiLifeReportAiService.java`, `OpenAiProperties.java`, `report-chat-system.txt`)가 **교집합 0개**였다. 단, 이는 그 시점 스냅샷일 뿐이므로 병합 전 §7.1의 셀프체크 명령으로 **실제 현재 main과 재확인**할 것.
- **병합 권장 방식**: 기능 커밋만 **squash merge 또는 cherry-pick**으로 가져오기(§7).
- **필요 설정**: Supabase 연결 정보 + 정부24 API 키(§8). Supabase에 이미 캐시 데이터가 적재되어 있어, 운영에서는 별도 수집 없이 바로 조회 가능.

---

## 1. 배경 & 목적

기존 서비스는 정부24 공공서비스 Open API를 **매 리포트 조회 때마다 실시간 호출**해 혜택 후보를 만들었다. 이 방식은:

- 외부 API 지연/실패에 리포트 응답이 통째로 흔들리고,
- 응답이 자유 텍스트라 "나이 조건이 맞는 혜택만"처럼 **구조화된 자격 판정이 불가능**했다.

이번 기능은 두 가지를 해결한다.

1. **DB 캐싱**: 정부24 혜택 데이터를 한 번 수집해 DB(`gov24_benefit_cache`)에 저장 → 런타임에는 외부 API 없이 DB에서 읽어 안정적으로 응답.
2. **맞춤형 매칭**: 각 혜택에 구조화된 자격조건(min_age/max_age 등)을 붙여, 사용자의 진단 정보와 대조해 **확정(confirmed) / 정보부족 보류(pending)** 로 나눠 추천.

---

## 2. 핵심 기능 (함께 정의·구현한 항목 전체)

### 2.1 정부24 데이터 DB 캐싱
- 정부24/보조금24 공공서비스 카탈로그(약 1만여 건)에서 퇴직·이직·실직·복지 관련 혜택을 수집해 `gov24_benefit_cache` 테이블에 저장.
- 각 행은 정부24 원본 응답 한 건을 `raw_json`(JSONB)으로 그대로 보관(한글 필드명 유지) → 원문 손실 없음.
- `Gov24PublicBenefitService`는 이제 외부 API 대신 이 테이블을 읽고, 30분 TTL 인메모리 캐시로 반복 조회를 줄인다.

### 2.2 구조화된 자격조건 컬럼
- `gov24_benefit_cache`에 다음 컬럼 추가: `min_age`, `max_age`, `min_insurance_months`, `min_tenure_years`, `is_involuntary_sub`.
- **"알 수 없는 값은 NULL"** 원칙: 지원대상/선정기준 원문에 신청자 본인의 나이가 **명확한 자격요건**으로 적힌 경우에만 값을 채웠다. 나이가 언급돼도 (a) 수급 "기간"에만 영향(예: 구직급여), (b) 고용주/자녀 등 신청자 본인과 무관, (c) 여러 자격 경로가 섞여 단일 범위로 왜곡되는 경우는 전부 NULL 유지.

### 2.3 사용자 맞춤 매칭 (confirmed / pending 분리)
- 사용자 진단값(나이·근속연수·고용보험 가입기간·이직사유)과 각 혜택의 구조화 조건을 대조:
  - **명백히 조건 밖** (예: 65세 전용 혜택인데 사용자 30세) → 후보에서 **제외**.
  - **조건은 있는데 사용자 값이 비어 있음** → **pending(보류)** 으로 분류하고, 어떤 값이 필요한지(`requiredForMatching`) 표시.
  - **조건 충족 / 조건 없음** → **confirmed** 로 추천 목록에 포함.

### 2.4 진단 보완 입력 PATCH API
- 나이·근속연수를 나중에 채워 넣을 수 있는 `PATCH /api/life/assessments/{id}` 추가.
- 값이 전달된 필드만 갱신(null은 무시). 재분석 없이 리포트를 다시 조회하면 최신 진단값으로 매칭이 재계산된다(추천은 리포트 조회 시점에 매번 계산).

### 2.5 나이 필수 입력화
- 나이는 실업급여 기간·퇴직금 판단뿐 아니라 **나이 조건 혜택 매칭의 핵심 키**이므로 진단 생성 시 필수(`@NotNull`)로 승격. 프론트 폼도 "나이(만) 필수" 배지 + 미입력 시 제출 버튼 비활성화.

### 2.6 추천 점수 가중치 보정
- 나이 등 구조화 조건을 **실제 사용자 값과 대조해 검증한 혜택**에는 점수 가중치(+50)를 부여. "긴급복지"처럼 텍스트만 겹치는 느슨한 매칭보다, 실제 자격을 검증한 혜택이 상위에 노출되도록 함.

### 2.7 리포트 화면 UI
- **보완 입력 배너**: `requiredForMatching`이 있으면 "나이를 입력하면 추가 혜택을 확인할 수 있어요" 배너 + `SupplementInputModal`(나이/근속연수 입력) 표시.
- **pending 섹션**: 정보 입력 시 확인 가능한 혜택 목록.
- **가로 스크롤 캐러셀**: 혜택 카드가 세로로 길게 늘어지지 않도록 좌우 스와이프 형태로 변경(카드 폭 축소 포함).

### 2.8 데모 모드 동기화
- 프론트 전용 데모 모드(`demo.ts`)도 동일한 confirmed/pending 판정 로직을 재현 → 백엔드 없이도 실제와 같은 동작을 시연.

### 2.9 데이터 정합성 검증
- 초기 큐레이션 5건을 실제 공식 기준과 대조 검증한 결과, **나이 조건이 허구이거나 제도 자체가 불확실한 3건을 제거**하고, 검증된 2건(청년 일자리 도약 장려금 15~34세, 국민내일배움카드 15~75세)만 `gov24_benefit_cache`에 병합. 기존 100여 건의 나이 조건도 원문 재검토 후 명확한 건만 채우고 나머지는 NULL 유지.

---

## 3. 아키텍처 & 데이터 흐름

```
[정부24 Open API]  --(1회 수집)-->  [gov24_benefit_cache 테이블]
                                          | (운영: Supabase Postgres / 로컬: H2)
                                          v
사용자 진단(age·tenure·insurance·reason)
                                          v
        Gov24PublicBenefitService.findBenefits(report)
          ├─ 30분 TTL 캐시로 테이블 로드
          ├─ 키워드 매칭 + 구조화 조건 대조
          ├─ 제외 / pending / confirmed 분류
          └─ PublicBenefitRecommendationService(AI 재랭킹) → 최종 순위
                                          v
      LifeReportResDTO { publicBenefits, pendingBenefits, requiredForMatching }
                                          v
            리포트 화면(추천 카드 + 보완 입력 배너/모달)
```

- 기존 룰 엔진(`RuleEngineService`), AI 재랭킹(`PublicBenefitRecommendationService`), 실업급여 룰(`UnemploymentBenefitRule`)의 **내부 판정 로직은 수정하지 않고 호출/재사용만** 했다. (단, `UnemploymentBenefitRule`에 비자발적 이직 판정을 재사용하기 위한 public static 헬퍼 1개만 추가.)

---

## 4. DB 스키마

### 4.1 신규 테이블: `gov24_benefit_cache`

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | bigint (PK) | 자동 증가 |
| `external_id` | text | 정부24 서비스ID |
| `title` | text | 서비스명 |
| `raw_json` | jsonb | 정부24 원본 응답 1건(한글 필드명 유지) |
| `min_age` | int (nullable) | 신청자 본인 최소 나이 자격(불명확 시 NULL) |
| `max_age` | int (nullable) | 신청자 본인 최대 나이 자격(불명확 시 NULL) |
| `min_insurance_months` | int (nullable) | 고용보험 최소 가입 개월 |
| `min_tenure_years` | int (nullable) | 최소 근속연수 |
| `is_involuntary_sub` | boolean (nullable) | 비자발적 이직 요건 여부 |

> 운영 Supabase에는 수집 파이프라인이 쓰던 부가 컬럼(`source_id`, `summary`, `body`, `source_url`, `published_at`, `fetched_at`, `content_hash`)도 존재하지만, **JPA 엔티티(`Gov24BenefitCache`)는 위 9개 컬럼만 매핑**한다. 나머지 컬럼은 매핑하지 않아도 조회에 영향 없음(추가 컬럼 무시).

- **운영(Supabase Postgres)**: 이미 캐시 데이터가 적재되어 있음. Hibernate `ddl-auto=update`로 테이블/컬럼 자동 생성·보강.
- **로컬(H2)**: `src/main/resources/data.sql`이 임베디드 DB에서만 자동 실행되어 동일 데이터를 시드(운영 Postgres에는 자동 실행되지 않음).

### 4.2 기존 테이블 변경 없음
- `life_assessment` 등 기존 테이블에 **컬럼 추가/삭제 없음**. (나이 필수화는 애플리케이션 검증 레벨이며 DB 제약 변경 아님.)

---

## 5. API 스펙

### 5.1 신규: 진단 보완 입력
```
PATCH /api/life/assessments/{assessmentId}
Body: { "age": number|null, "tenureYears": number|null }   // 둘 다 선택, 전달된 값만 갱신
Res : LifeAssessmentResDTO
```

### 5.2 변경: 진단 생성 (필드 제약만 강화)
```
POST /api/life/assessments
- age 필드가 @NotNull 로 승격 (기존 필수 항목 구성은 그대로, 나이만 필수로 추가)
```

### 5.3 변경: 리포트 상세 응답 (필드 추가, 하위호환)
```
GET /api/life/reports/{reportId}
LifeReportResDTO 에 필드 추가:
  - pendingBenefits: PublicBenefit[]      // 정보 입력 시 확인 가능한 혜택
  - requiredForMatching: string[]         // 예: ["age"], ["tenureYears"]
PublicBenefitResDTO 에 필드 추가:
  - sourceType: "DB" | "GOV24_API"
```
> 모두 **필드 추가(가법)** 이므로 기존 소비자에 하위호환. 프론트 타입도 동일하게 확장.

---

## 6. 변경 파일 목록

### 6.1 신규 파일 (충돌 불가 — 새 파일)
```
src/main/java/.../lifetransition/model/Gov24BenefitCache.java
src/main/java/.../lifetransition/repository/Gov24BenefitCacheRepository.java
src/main/java/.../lifetransition/dto/request/LifeAssessmentPatchReqDTO.java
src/main/java/.../lifetransition/enumtype/PublicBenefitSourceType.java
src/main/resources/data.sql                       # 로컬 H2 시드
scripts/run-backend.ps1                           # 로컬 실행 헬퍼(선택)
docs/PRD-로컬-실행-환경-구축.md                     # 문서
docs/PRD-정부24-공공혜택-추천-병합요청.md            # 본 문서
```

### 6.2 수정 파일 — 백엔드 (모두 가법: 필드/메서드 추가)
```
controller/LifeAssessmentController.java          # +PATCH 엔드포인트 메서드
dto/request/LifeAssessmentCreateReqDTO.java        # age 에 @NotNull 1줄
dto/response/LifeReportResDTO.java                 # +pendingBenefits, +requiredForMatching, +내부 record
dto/response/PublicBenefitResDTO.java              # +sourceType 필드
model/LifeAssessment.java                          # +updatePartial(...) 메서드
rule/rules/UnemploymentBenefitRule.java            # +isInvoluntaryReason(...) static 헬퍼
service/LifeAssessmentService.java                 # +updatePartial(...) 메서드
application-local.properties                       # +defer-datasource-initialization=true (로컬 전용)
```

### 6.3 수정 파일 — 백엔드 (대규모 리라이트, 단 단일 소유)
```
service/Gov24PublicBenefitService.java             # 라이브 API 호출 → DB 캐시 조회 + confirmed/pending + 점수 가중치
```
> 이 서비스는 **이 기능이 단독 소유**하는 파일이다(참고 스냅샷의 main은 미변경). main이 별도로 공공혜택 추천 로직을 고치지 않았다면, 리라이트 규모가 커도 충돌하지 않는다. 병합 전 §7.1 셀프체크로 이 파일이 겹치는지 확인 권장.

### 6.4 수정 파일 — 프론트엔드 (잠재 충돌 지점)
```
frontend/src/lib/types.ts                          # 타입 필드 추가
frontend/src/lib/api.ts                            # patchAssessment() 추가
frontend/src/lib/demo.ts                           # 데모 매칭 로직 대폭 추가
frontend/src/app/assessment/new/page.tsx           # 나이 필수화 UI
frontend/src/app/globals.css                       # .pb-* 캐러셀/카드 스타일 추가
frontend/src/app/report/[id]/page.tsx              # pending 섹션/보완 모달/배너 추가
```

---

## 7. `main` 충돌 방지 가이드 ⭐ (가장 중요)

> **전제**: 이 문서는 `main`의 최신 상태를 알지 못한 채 작성됐다. `main`은 계속 발전 중이라, 아래 참고 스냅샷 이후 더 많은 기능이 들어갔을 수 있다. 그래서 이 장은 **① main 상태와 무관하게 성립하는 구조적 안전성**, **② 병합 시점에 실제 main과 겹치는지 직접 확인하는 명령**, **③ 겹칠 경우의 해결 원칙** 순서로 제공한다.

### 7.1 먼저: 실제 main과의 겹침을 직접 확인 (셀프체크)
스냅샷을 믿지 말고, **병합 담당자가 자신의 현재 main 기준으로** 실제 겹치는 파일을 뽑아본다.

```bash
# 이 기능 브랜치를 로컬로 가져온 뒤:
git fetch <feature-remote> feature/gov24-benefit-cache

# (A) 이 기능이 바꾼 파일 목록
git diff --name-only $(git merge-base main feature/gov24-benefit-cache) feature/gov24-benefit-cache > /tmp/feature_files.txt

# (B) 공통 조상 이후 main이 바꾼 파일 목록
git diff --name-only $(git merge-base main feature/gov24-benefit-cache) main > /tmp/main_files.txt

# (C) 교집합 = 실제 충돌 위험 파일
comm -12 <(sort /tmp/feature_files.txt) <(sort /tmp/main_files.txt)
```
> (C)의 출력이 **비어 있으면 파일 단위 충돌 없음** → 그냥 merge/cherry-pick 하면 된다. 출력이 있으면, 그 파일들만 §7.4의 원칙으로 처리하면 된다. (드라이런: `git merge --no-commit --no-ff <branch>` 후 결과 확인, 그리고 `git merge --abort`로 되돌리기.)

### 7.2 구조적으로 안전한 부분 (main이 얼마나 나갔든 무관)
아래는 main의 상태와 관계없이 충돌 가능성이 거의 없다.

- **신규 파일 8개** (§6.1): 같은 경로의 파일을 main이 **새로 만들지 않은 한** 충돌 불가.
  - 유일한 현실적 예외: `src/main/resources/data.sql` — main도 자체 시드 파일을 추가했다면 같은 경로가 겹칠 수 있다. 이 경우 **두 파일의 INSERT/UPDATE 문을 이어붙이면** 됨(서로 다른 테이블이라 내용 충돌 없음).
- **백엔드 가법 수정** (§6.2): DTO 필드 추가, 서비스 메서드 추가, 컨트롤러 엔드포인트 추가. main이 **정확히 같은 줄 근처**를 고치지 않는 한 자동 병합된다.
- **핵심 로직 집중**: 추천 판정 로직 전체가 `Gov24PublicBenefitService` 한 파일에 모여 있고, 이 파일은 이 기능이 사실상 단독 소유한다(참고 스냅샷의 main은 미변경). main이 별도로 "공공혜택 추천" 로직을 새로 짜지 않았다면 겹치지 않는다.

### 7.3 병합 권장 방식
이 브랜치에는 본 기능 외에 예전 소셜로그인 관련 커밋이 앞에 섞여 있을 수 있다. 가장 깨끗한 방법은 **기능 커밋만 분리**해 가져오는 것:

```bash
git checkout main && git pull
git cherry-pick <기능 커밋 해시>   # "정부24 공공혜택 DB 저장 및 사용자 맞춤형 추천 기능 추가"
# 또는 PR에서 "Squash and merge"
```
> 이렇게 하면 부수 커밋이 딸려오지 않고 **정확히 기능 파일만** main에 얹힌다. (충돌이 나도 이 방식이 원인 파일을 최소화한다.)

### 7.4 겹칠 경우 해결 원칙 — "양쪽 유지(Accept Both)"
§7.1 (C)에서 겹치는 파일이 나왔다면, 아래를 참고한다. 이 기능의 변경은 **기존 코드를 덮어쓰지 않고 옆에 새로 붙이는 성격**이라, 대부분 양쪽을 살리면 해결된다.

| 파일(겹칠 경우) | 이 기능이 추가한 것 | 해결 지침 |
|---|---|---|
| `globals.css` | `.pb-swipe-hint`, `.pb-grid`(가로스크롤), `.pb-card`(폭 축소) 등 **신규 클래스** | main 스타일 유지 + 신규 `.pb-*` 블록을 그대로 덧붙임 |
| `report/[id]/page.tsx` | `PendingBenefitSection`, `SupplementInputModal` **신규 컴포넌트** + 배너/pending 렌더 삽입 | main 화면 유지 + 신규 컴포넌트 정의 + `<PublicBenefitSection>` 근처에 pending/배너 렌더 추가 |
| `demo.ts` | `DEMO_BENEFIT_MASTER`, `buildDemoBenefits()`, `patchAssessment()` 등 **신규 함수/상수** | main 데모 유지 + 신규 심볼 추가 |
| `types.ts` / `api.ts` | 타입 필드(`sourceType`, `pendingBenefits`, `requiredForMatching`), `patchAssessment()` | 추가만 반영 |
| `assessment/new/page.tsx` | 나이 필수화 검증 + 라벨 | 제출 검증 조건에 `age` 추가, 라벨에 "필수" 배지 |
| `LifeReportResDTO.java` / `PublicBenefitResDTO.java` | 응답 필드 추가 | 필드 추가만 반영(생성자 인자 순서 주의) |
| `Gov24PublicBenefitService.java` | 대규모 리라이트(라이브 API→DB 캐시) | main이 이 파일을 크게 고쳤다면 **이 기능 버전으로 대체**하되, main이 추가한 별도 로직이 있으면 통합 필요 — 개발자에게 문의 |

> 판단 기준: 충돌 마커가 뜨면 기본은 "양쪽 유지"로 두고, **이 기능의 심볼(`.pb-*`, `PendingBenefitSection`, `SupplementInputModal`, `buildDemoBenefits`, `patchAssessment`, `pendingBenefits`, `requiredForMatching`, `sourceType`)이 최종본에 살아있는지**만 확인하면 된다.

### 7.5 병합 후 필수 확인
- 백엔드 컴파일: `./gradlew compileJava`
- 프론트 타입체크: `npm run build` 또는 `npx tsc --noEmit`
- 리포트 화면에서 (a) 나이 미입력 시 pending 배너, (b) 나이 입력 후 confirmed 목록 갱신 동작.

---

## 8. 환경변수 / 배포 설정

### 8.1 Supabase (공공혜택 캐시 저장소)
운영 DB로 Supabase Postgres를 사용한다. 아래 두 값은 **Supabase 프로젝트 식별 정보**다.

```
SUPABASE_URL=https://xqxwjannhcjueqlhfcww.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhxeHdqYW5uaGNqdWVxbGhmY3d3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI4NzEwODAsImV4cCI6MjA5ODQ0NzA4MH0.XWmJi-FI2467E_m0nOMkWFUgKlbmS8EVNbUfmN0Yihc
```

> ⚠️ **정확히 짚고 넘어갈 점**: 위 `SUPABASE_ANON_KEY`는 Supabase의 **anon(public) 키**로, Supabase REST/Client SDK·대시보드·MCP 도구용이다. **Spring 백엔드의 런타임 DB 조회는 이 키를 쓰지 않고 아래 JDBC 연결(§8.2)을 쓴다.** 즉 anon 키는 참고/클라이언트 도구용으로 보관하는 값이다. (service_role 등 비밀 키는 절대 리포지토리에 커밋하지 말 것.)
> ⚠️ 이 값들은 `.env`(gitignore 대상)에만 넣고 커밋하지 않는다.

### 8.2 백엔드 런타임 DB 연결 (실제 캐시 조회 경로)
`gov24_benefit_cache` 테이블은 아래 **JDBC 연결**로 읽는다. Supabase 프로젝트 > Settings > Database에서 연결 문자열/비밀번호를 확인해 채운다.

```
SPRING_DATASOURCE_URL=jdbc:postgresql://<supabase-db-host>:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<supabase-db-password>
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

### 8.3 정부24 API 키 (캐시 최초/갱신 수집 시에만 필요)
런타임 추천은 DB에서 읽으므로 이 키가 없어도 동작한다. **캐시 데이터를 새로 수집/갱신할 때만** 필요.
```
GOV24_PUBLIC_SERVICE_ENABLED=true
GOV24_PUBLIC_SERVICE_KEY=<공공데이터포털 발급 키>
GOV24_PUBLIC_SERVICE_MAX_RESULTS=50     # (기본 6) 노출 후보 상한 — 필요 시 상향
GOV24_PUBLIC_SERVICE_MAX_KEYWORDS=10    # (기본 5)
```

### 8.4 로컬 개발
- `spring.profiles.active=local` → H2 인메모리 + `data.sql` 자동 시드. Supabase/정부24 키 없이도 전 기능 시연 가능.
- `application-local.properties`에 `spring.jpa.defer-datasource-initialization=true` 추가되어 Hibernate가 테이블을 만든 뒤 시드가 들어간다(로컬 전용, 운영 무영향).

---

## 9. 로컬 실행 방법 (리뷰어용)

```bash
# 백엔드 (H2, 키 불필요)
./gradlew bootRun --args="--spring.profiles.active=local"

# 프론트엔드
cd frontend && npm install && npm run dev
```
- 데모 로그인 → 실직 진단(나이 미입력) → 결제(데모) → 리포트에서 pending 배너 확인 → 나이 입력 → confirmed 목록 갱신 확인.

---

## 10. 검증 & 테스트 결과

- 백엔드 `compileJava` / 프론트 `tsc --noEmit` **통과**.
- 시나리오 검증(로컬 실측):
  - **나이 미입력**: 나이 조건 있는 혜택이 정확히 pending으로 분류(`requiredForMatching=["age"]`), 배너 노출.
  - **나이=30 입력**: 청년/취업 계열 혜택이 pending→confirmed 이동, 65세 전용 혜택은 조건 불충족으로 제외, 배너 사라짐.
  - **나이=70 입력**: 노인 대상 혜택 confirmed로 노출, 청년 전용 혜택 제외. 점수 가중치로 검증된 혜택(국민내일배움카드 등)이 상위 노출.
- 데이터 정합성: 부정확 3건 제거, 나머지 나이 조건 원문 재검토 후 명확한 건만 반영(§2.9).

---

## 11. 리스크 & 롤백

| 항목 | 내용 | 대응 |
|---|---|---|
| 나이 필수화 | 기존 클라이언트가 나이 없이 진단 생성 시 400 | 프론트 폼도 필수화 반영 완료. 정책상 나이는 매칭 핵심 키라 필수 타당 |
| Supabase 의존 | 운영 캐시 미적재 시 추천 비게 됨 | 이미 적재 완료. 재수집은 정부24 키로 언제든 가능 |
| 대규모 리라이트 파일 | `Gov24PublicBenefitService` | 참고 스냅샷에선 main 미변경. main이 이 파일을 별도로 고쳤을 경우 §7.4 참고. 롤백은 해당 커밋 revert로 격리 가능 |
| 프론트 충돌 | UI 파일(`globals.css`, `report page`, `demo.ts` 등) | main 상태에 따라 겹칠 수 있음 → §7.1 셀프체크로 확인 후 §7.4 "양쪽 유지"로 해결 |
| main 상태 미상 | 본 문서는 main 최신본을 모른 채 작성 | 병합 전 §7.1 명령으로 실제 겹침 파일을 리뷰어가 직접 산출 |

- **롤백**: 기능 커밋 단위로 격리되어 있어 `git revert <기능 커밋>` 한 번으로 되돌릴 수 있다(신규 파일 삭제 + 가법 변경 원복).

---

## 12. 병합 체크리스트

- [ ] §7.1 셀프체크로 실제 main과 겹치는 파일 산출
- [ ] 기능 커밋 cherry-pick 또는 브랜치 squash merge (§7.3)
- [ ] `.env`에 Supabase JDBC 연결 + (선택)정부24 키 설정 (§8)
- [ ] `./gradlew compileJava` 통과
- [ ] 프론트 `npm run build` 통과
- [ ] 리포트 화면 pending→confirmed 동작 확인 (§9)
- [ ] `SUPABASE_ANON_KEY` 등 비밀값이 커밋에 포함되지 않았는지 확인
