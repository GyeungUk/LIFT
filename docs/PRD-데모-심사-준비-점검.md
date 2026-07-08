# PRD - LIFT 데모 심사 준비 점검

작성일: 2026-07-08
대상 프로젝트: `/Users/gimgyeong-ug/Documents/LIFT`
근거 문서: `/Users/gimgyeong-ug/Documents/LIFT 2/tmp/pdfs/share-summary/LIFT_프로젝트_정리본.html`, `/Users/gimgyeong-ug/Documents/LIFT 2/tmp/pdfs/cost-analysis/LIFT_운영비_수익성_계산.html`

## 1. 제품 정의

LIFT는 퇴직, 이직처럼 생애 상태가 바뀌는 순간 사용자가 놓치기 쉬운 행정 절차, 신청 기한, 필요 서류, 예상 금액, 공식 신청 링크를 정리해 주는 생애전환 행정 준비 플랫폼이다.

MVP의 핵심은 자동 신청이나 행정 판단 대행이 아니라, 사용자가 입력한 최소 정보와 서버 룰 엔진을 기반으로 "지금 무엇을 먼저 확인해야 하는지"를 리포트로 보여 주는 것이다.

## 2. 핵심 사용자

- 퇴직 예정자 또는 퇴직 직후 사용자
- 이직 중이며 고용보험, 건강보험, 국민연금 공백을 확인해야 하는 사용자
- 결제 전 내가 확인할 수 있는 항목이 있는지 먼저 보고 싶은 사용자
- 모바일에서 리포트와 PDF 저장 흐름을 확인하려는 데모/심사 사용자

## 3. MVP 요구사항

### 3.1 인증 및 진입

- 사용자는 카카오 또는 네이버 소셜 로그인으로 진입할 수 있어야 한다.
- 데모용 빠른 로그인은 별도 버튼으로 제공할 수 있다.
- `/login`, `/terms`, `/privacy`는 공개 URL에서 200으로 열려야 한다.
- 소셜 로그인 검수 시 카카오/네이버 버튼은 실제 제공자 동의 화면으로 이동해야 한다.

### 3.2 온보딩

- 현재 선택 가능한 생애 이벤트는 `퇴직`, `이직`으로 제한한다.
- `결혼`, `출산휴가`는 준비 중 카드로만 보여 주고 선택할 수 없어야 한다.
- 약관과 개인정보처리방침 동의 후 진단 입력으로 이동한다.

### 3.3 진단 입력

- 필수 입력: 퇴직/마지막 근무일, 퇴사 사유, 다음 일자리 상태, 고용보험 가입 개월 수, 현재 소득 상태, 나이, 거주 지역.
- 다음 일자리 상태가 `확정됨`이면 시작일을 입력할 수 있어야 한다.
- `권고사직`은 신규 사용자 선택지에서 제외하고, `정년퇴직`은 선택지에 포함한다.
- 월 평균임금은 민감 정보이므로 PDF 저장 시점에 별도 입력받을 수 있다.

### 3.4 룰 엔진 및 리포트

- 서버 룰 엔진이 실업급여, 건강보험 임의계속가입, 국민연금 납부예외, 세금/퇴직금 체크리스트를 계산한다.
- AI는 자격 판단자가 아니라 룰 엔진 결과를 설명하는 보조 역할만 한다.
- 미리보기에서는 전체 항목 수와 일부 핵심 항목을 보여 주고, 결제 후 전체 리포트를 연다.
- 공식 신청은 정부/공공기관 사이트로 연결한다.

### 3.5 결제 및 상품

- 결제 전 신청 가능 또는 확인 필요 항목 수를 보여 준다.
- 신청 가능 항목이 0개이면 결제 버튼을 비활성화한다.
- 플랜은 `기본 리포트 6,900원`, `확장 리포트 13,900원`으로 나눈다.
- 데모 결제는 즉시 결제 완료 처리할 수 있고, 토스 테스트 결제는 테스트 키가 있을 때만 활성화한다.

### 3.6 PDF, 서류, AI 질문

- 확장 리포트 결제자는 PDF 저장 화면을 사용할 수 있어야 한다.
- PDF 저장은 월급 입력/미입력 두 경로를 제공한다.
- 필요 서류 조회는 데모에서는 mock으로 동작해도 되지만, 화면 문구는 실제 공공 마이데이터 연동처럼 오해되지 않게 해야 한다.
- 확장 리포트 결제자는 리포트 기반 AI 질문 10회를 사용할 수 있어야 한다.
- OpenAI가 꺼진 환경에서는 서버 fallback 답변으로 500 없이 동작해야 한다.

## 4. 현재 구현 비교

| 영역 | PRD 기준 | 현재 상태 | 판단 |
|---|---|---|---|
| 프론트 공개 페이지 | `/login`, `/terms`, `/privacy` 200 | 라이브 URL에서 모두 200 확인 | 통과 |
| 백엔드 빌드 | 배포 전 빌드/테스트 통과 | `./gradlew clean build --no-daemon` 성공 | 통과 |
| 프론트 빌드 | Next production build 통과 | `npm run build` 성공 | 통과 |
| 프론트 lint | 기본 lint 통과 | `npm run lint` 성공 | 통과 |
| 로컬 서버 기동 | Spring Boot 서버 정상 기동 | local profile, 18080/18081 포트 기동 성공 | 통과 |
| 온보딩 이벤트 | 퇴직/이직만 선택, 결혼/출산휴가 잠금 | 구현됨 | 통과 |
| 퇴사 사유 | 권고사직 제거, 정년퇴직 추가 | 구현됨 | 통과 |
| 다음 일자리 시작일 | 확정 시 달력 표시 | 구현됨 | 통과 |
| 결제 전 항목 확인 | actionable count로 결제 제한 | 구현됨 | 통과 |
| 플랜 가격 | BASIC 6,900원, PLUS 13,900원 | 프론트/서버 일치 | 통과 |
| PDF 저장 | 확장 리포트에서 월급 입력/미입력 경로 | 구현됨, API 200 확인 | 통과 |
| 서류 조회 | 데모 mock 가능 | mock 조회 API 200 확인 | 통과, 표현 주의 |
| 라이브 소셜 로그인 | 실제 카카오/네이버 동의 화면 이동 | 라이브 API가 `mock-kakao-login`, `mock-naver-login`으로 바로 콜백 | 실패 |
| 라이브 AI 채팅 | 확장 리포트 결제 후 질문 가능 | 라이브에서 `POST /chat/messages`가 `COMMON500_1` | 실패 |
| OpenAI on 환경 | 실제 AI 연동 시 500 없어야 함 | `LocalDate` 직렬화 실패로 500 재현 | 실패 |
| Render 설정 일치 | 운영에서는 mock OAuth false | `render.yaml`은 false이나 실제 라이브는 mock 동작 | 환경 불일치 |
| cold start | 심사자가 기다릴 수 있는 수준 | 첫 백엔드 응답이 약 90초 이상 지연 | 리스크 |

## 5. 검증 로그 요약

### 로컬 빌드

- `./gradlew clean build --no-daemon`: 성공
- `npm run build`: 성공
- `npm run lint`: 성공

### 로컬 API E2E

OpenAI가 켜진 local profile 18080:

- 약관 동의: OK
- 진단 생성: OK
- 분석/미리보기: OK
- PLUS 데모 결제: OK
- 상세 리포트: OK
- PDF estimate: OK
- 서류 조회: OK
- AI 채팅: 실패, HTTP 500 `COMMON500_1`

OpenAI를 강제로 끈 local profile 18081:

- 동일한 PLUS 결제 후 AI 채팅: OK
- 남은 질문 수 9회 확인

### 라이브 API E2E

공개 API `https://lift-api-c2z6.onrender.com`:

- `/api/auth/login/kakao`: 실제 카카오가 아니라 mock callback으로 이동
- `/api/auth/login/naver`: 실제 네이버가 아니라 mock callback으로 이동
- mock callback 기반 로그인 후 약관, 진단, 분석, PLUS 데모 결제, 상세 리포트: OK
- AI 채팅: 실패, `COMMON500_1 서버 에러입니다.`

## 6. 심사에서 거의 확실히 걸릴 항목

### 6.1 라이브 소셜 로그인 mock 동작

카카오/네이버 버튼이 실제 제공자 동의 화면으로 가지 않고 `mock-...-login` 콜백으로 바로 돌아온다. 소셜 로그인 검수나 AI 심사에서 로그인 버튼을 클릭하면 바로 확인된다.

원인 후보:

- Render 실제 환경변수에서 `OAUTH_MOCK_ENABLED=true`가 켜져 있음
- 또는 Kakao/Naver client id/secret이 비어 있어 mock 경로로 빠짐
- `render.yaml`에는 `OAUTH_MOCK_ENABLED=false`로 적혀 있으므로 실제 Render 환경과 파일 설정이 어긋난 상태

### 6.2 PLUS 리포트 AI 채팅 500

라이브 배포에서 PLUS 결제 후 AI 질문을 보내면 `COMMON500_1`이 반환된다. 로컬에서도 OpenAI를 켠 환경에서 동일하게 재현된다.

확인된 직접 원인:

- `OpenAiLifeReportAiService`가 `new ObjectMapper()`를 직접 만들고 있다.
- 이 ObjectMapper에 Java time 모듈이 없어 `LocalDate retirementDate` 직렬화에 실패한다.
- 실패 위치는 `OpenAiLifeReportAiService.generateAnswer()`의 `objectMapper.writeValueAsString(userContent)`이다.

임시 회피:

- 심사/데모 전 `OPENAI_ENABLED=false`로 배포하면 fallback 챗봇은 정상 동작한다.

권장 수정:

- Spring이 관리하는 `ObjectMapper`를 주입받거나 `JavaTimeModule`을 등록한다.
- 또는 `retirementDate`, `nextJobStartDate`를 map에 넣을 때 문자열로 변환한다.

### 6.3 백엔드 cold start 지연

Render 무료 플랜으로 보이며, 첫 소셜 로그인 요청이 90초 이상 지연됐다. 심사자가 짧게만 기다리면 "로그인 안 됨"으로 판단할 수 있다.

권장 대응:

- 심사 시간 전 백엔드를 미리 깨워 둔다.
- 가능하면 심사 기간에는 유료/always-on 플랜을 사용한다.
- 프론트에 "서버 깨우는 중" 상태를 명시하는 것도 데모 방어에 도움이 된다.

## 7. 데모라면 허용 가능하지만 질문받을 수 있는 항목

- 토스 테스트 결제는 키가 없으면 버튼이 비활성화된다. 데모 결제를 주 경로로 보여 주면 괜찮다.
- 정부24/공공혜택 연동은 `render.yaml`에서 꺼져 있다. "공공데이터 연동"을 시연하려면 해당 기능을 켜거나, 데모에서는 룰 엔진 중심이라고 설명해야 한다.
- 필요 서류 자동 조회는 mock이다. 화면 문구가 "공공 마이데이터로 자동 조회"처럼 보이면 실제 연동으로 오해받을 수 있다.
- 토큰이 localStorage에 저장된다. 데모 심사에서는 보통 치명적이지 않지만, 보안 심사가 엄격하면 httpOnly 쿠키 권고가 나올 수 있다.
- `/terms`, `/privacy`는 열리지만 운영자 정보, 문의 채널, 파기 절차 등은 간단하다. 소셜 로그인 검수용으로는 더 구체화하면 안전하다.

## 8. 배포 가능성 판단

빌드와 기본 리포트 생성/결제/PDF 흐름만 보면 배포 자체는 가능하다. 하지만 현재 공개 배포 상태 기준으로는 "심사 제출 직전"이라고 보기는 어렵다.

최소 수정 없이 데모를 진행할 경우 안전한 시연 경로:

1. 프론트 `/login` 접속
2. `데모용 로그인으로 바로 체험하기`
3. 퇴직/이직 선택
4. 진단 입력
5. 미리보기
6. PLUS 데모 결제
7. 상세 리포트
8. PDF 저장 또는 서류 mock 조회

피해야 할 시연 경로:

1. 카카오/네이버 실제 로그인 심사
2. 라이브 PLUS 리포트 AI 채팅
3. cold start 직후 바로 로그인 버튼 클릭

## 9. 우선순위 액션

1. `OpenAiLifeReportAiService`의 `LocalDate` 직렬화 500을 수정하거나, 심사 배포에서는 `OPENAI_ENABLED=false`로 고정한다.
2. Render 실제 환경변수를 확인해 `OAUTH_MOCK_ENABLED=false`, Kakao/Naver client id/secret, redirect uri가 제대로 들어갔는지 맞춘다.
3. 카카오/네이버 개발자센터에 등록된 redirect uri가 `https://life-shift.netlify.app/login/callback/{provider}`와 일치하는지 확인한다.
4. 심사 전 Render 서버를 미리 깨우거나 always-on 환경으로 바꾼다.
5. 실제 연동이 아닌 기능에는 "데모" 또는 "예시" 문구를 붙여 과장 표현 리스크를 줄인다.
