# 프론트엔드 개발 환경

React와 TypeScript를 사용하는 Vite 앱입니다. 백엔드와 같은 저장소의 일반 디렉터리이며,
패키지·잠금 파일·개발 도구 설정은 모두 `frontend/` 안에서 독립적으로 관리합니다.

작업 전 [개발 컨벤션](FRONTEND.md), [화면 정합성 규칙](notes/page-families.md),
[에이전트 작업 규칙](AGENTS.md)을 확인합니다.

## 설치

저장소 루트에서 `frontend/`로 이동한 뒤 실행합니다. Node.js 버전은 이 디렉터리의 `.nvmrc`,
pnpm 버전은 `package.json`의 `packageManager`에 고정합니다.

```bash
cd frontend
nvm install
nvm use
npm install --global pnpm@12.16.1
pnpm install --frozen-lockfile
```

현재 기준은 Node.js 24.18.0, pnpm 12.16.1입니다. nvm 이외의 도구로 Node.js를 관리한다면
동일한 버전을 준비합니다. 기존 pnpm이 있다면 프로젝트에서 고정한 버전을 사용하는지 확인합니다.
`devEngines.runtime`에도 Node 24.18.0을 고정했습니다. `pnpm install`은 처음 한 번 필요한 런타임을
설치하고 이후 프로젝트 명령에 사용하므로, 터미널의 기본 Node가 20이어도 pnpm 명령은 Node 24로
실행됩니다. `.mts` 실행에서 `ERR_UNKNOWN_FILE_EXTENSION`이 발생하면 `pnpm install` 후 다시
실행하거나 `.nvmrc`에 맞춰 `nvm use`를 실행합니다.
의존성을 추가·갱신할 때는 `pnpm install`로 이 디렉터리의 `pnpm-lock.yaml`도 함께 갱신합니다.
`pnpm-workspace.yaml`은 이 앱의 pnpm 설치 정책 설정이며 루트 JavaScript workspace를 만들지 않습니다.

## 개발 및 검증

아래 명령은 모두 `frontend/`에서 실행합니다.

| 명령                    | 용도                                                                |
| ----------------------- | ------------------------------------------------------------------- |
| `pnpm dev`              | 개발 서버 실행: `http://localhost:5173`                             |
| `pnpm typecheck`        | 앱과 Vite 설정의 TypeScript 검사                                    |
| `pnpm lint`             | ESLint 검사, 경고도 실패로 처리                                     |
| `pnpm ui:check`         | 공용 CSS 소유권·동일 규칙의 중복 선언 검사                          |
| `pnpm i18n:check`       | 한국어·영어 번역 키·치환 변수 일치 검사                             |
| `pnpm test:routing`     | 기존 미리보기 URL의 화면 경로 전환·우선순위 검사                    |
| `pnpm test:csrf`        | CSRF 쿠키 준비·공통 헤더 전달·동시 요청·취소·재전송 방지 검사       |
| `pnpm test:auth`        | 로그인·메모리 토큰·동시 갱신·복원·로그아웃·인증 변경 경합 검사      |
| `pnpm test:api`         | 명세 기반 라우팅·엔드포인트 확장·핵심 응답 검증·생성·호출 경계 검사 |
| `pnpm test:preferences` | 테마 저장·복원·시스템 연동과 지역화 회귀 검사                       |
| `pnpm format:check`     | 공통 설정·프론트엔드 포맷 검사                                      |
| `pnpm format`           | 공통 설정·프론트엔드 포맷 적용                                      |
| `pnpm check`            | 타입·ESLint·UI 스타일·번역·설정·라우팅 회귀·포맷 검사 순서대로 실행 |
| `pnpm build`            | 타입 검사 후 배포용 파일을 `frontend/dist/`에 생성                  |
| `pnpm preview`          | 빌드 결과 미리보기: `http://localhost:4173`                         |

개발 서버와 미리보기는 지정 포트를 사용합니다. 포트가 사용 중이면 다른 포트로 자동 이동하지 않고
실패하므로 기존 프로세스를 확인하거나 `pnpm dev --port 5174`처럼 포트를 지정합니다.
`pnpm preview`는 먼저 `pnpm build`를 실행해야 하며 운영 서버로 사용하지 않습니다.
`frontend/` 안에서는 `pnpm dev`, `pnpm typecheck`, `pnpm lint`, `pnpm build`, `pnpm preview`를
직접 실행할 수 있습니다. 저장소 루트에는 프론트엔드 명령이나 Node 의존성을 두지 않습니다.

### 자동 CI

`Frontend CI`는 `dev`·`main` 대상 PR과 `dev` push에서 프론트 코드·설정·정적 자원 또는
`.github/workflows/frontend-ci.yml`이 바뀌면 실행합니다. Markdown(`*.md`)과 `notes/`만 바뀌면
실행하지 않습니다. 백엔드 코드만 바뀌면 기존 `Backend CI`가 실행됩니다.

Node.js는 `.nvmrc`, pnpm은 `package.json`의 고정 버전을 사용하며 잠금 파일 기준으로 pnpm 저장소를
캐시합니다. 설치·검사·빌드는 별도 단계로 실행하므로 실패한 단계와 세부 명령은 Actions 로그에서
확인할 수 있습니다. 로컬에서도 위 버전을 준비하고 `frontend/`에서 같은 순서로 재현합니다.

```bash
pnpm install --frozen-lockfile
pnpm check
pnpm build
```

검사는 저장된 OpenAPI 계약을 사용하므로 실행 중인 백엔드·DB·개인 환경변수가 필요하지 않습니다.

### 백엔드와 함께 빌드·실행

배포 시에는 저장소 루트에서 `./backend/gradlew -p backend clean build -PincludeFrontend`를 실행하면
프론트 설치·타입 검사·빌드 후 Spring Boot JAR에 화면을 포함합니다. 결과는
`backend/build/generated/frontend/`에 생성하며 `frontend/dist/`를 사용하지 않습니다.
백엔드 실행 후 같은 주소의 `/`에서 화면을 열 수 있습니다.
설정·실행·화면 경로 정책은 [통합 빌드 안내](../backend/README.md#프론트엔드를-포함한-배포용-빌드)를 따릅니다.
로컬에서 함께 실행할 때는 `./backend/gradlew -p backend bootRun -PincludeFrontend --args='--spring.profiles.active=local'`을
저장소 루트에서 사용합니다. IntelliJ Run은 `frontendBuild` 또는 통합 빌드로 결과를 준비한 뒤
`local` 프로필·`backend/` 작업 디렉터리로 실행하면 같은 화면을 제공합니다.

## 구성 및 범위

- `src/main.tsx`: React 시작점, 개발 중 StrictMode 적용
- `src/App.tsx`: 앱 셸·모달·테마 설정과 페이지 로딩 연결
- `src/routing/`: React Router 주소 구성·기존 URL 전환·이동 후 포커스와 안내
- `src/pages/status/`: 404·실행 오류·로딩 페이지 및 라우트 오류 경계
- `src/pages/`: 홈·여행·동행·내 방·브랜드 보기의 정적 초안
- `src/components/ui/`: 공용 버튼·입력·드롭다운·태그·모달·툴팁·단축키 표시
- `src/components/features/`: 로그인 모달·프로필 메뉴·설정 미리보기
- `src/components/layout/`: 데스크톱 상단·모바일 하단 메뉴와 앱 셸
- `src/data/preview.ts`: 업무 API와 연결하지 않는 예시 데이터
- `public/brand/`: SVG·PNG 로고·심볼·워드마크·앱 아이콘과 공식 로그인 제공자 자산
- `public/assets/theme.js`: 첫 화면 전 테마 적용, 저장·복원·시스템 변경 구독
- `src/hooks/useTheme.ts`: 브라우저와 React가 공유하는 테마 상태
- `src/styles/app.css`: 공통 스타일·색상·간격·크기 토큰
- `src/locales/ko.json`: 기본 한국어 문구
- `src/locales/en.json`: 영어 문구
- `src/i18n/`: i18next·React 연결, 번역 키 타입과 문서 언어 동기화
- `tsconfig.*.json`: 앱·개발 도구 설정 분리, strict 타입 검사
- `vite.config.ts`: React 플러그인, `@/` → `src/` 별칭과 포트 설정

현재 기본 화면은 API나 개인 환경변수 없이 실행됩니다. `VITE_`로 시작하는 환경변수는
브라우저에 노출되므로 비밀번호·서명 키 등 비밀값을 넣지 않습니다. 개인 `.env` 파일,
`node_modules/`, `dist/`는 Git으로 관리하지 않습니다.

## 기본 언어와 번역

기본 언어와 대체 언어는 한국어(`ko`)이며 영어(`en`)를 지원합니다. 저장된 선택이 없거나
지원하지 않는 값이면 브라우저 언어와 무관하게 한국어로 시작합니다. 헤더의 언어 버튼이나
프로필 → 설정의 언어 메뉴로 변경하며 `src/i18n/index.ts`의 `setLanguage()`가 문구·HTML `lang`·
문서 설명과 저장 값을 함께 갱신합니다. 다른 탭의 변경도 반영합니다.

새 문구는 두 JSON 사전에 같은 키로 추가합니다. 한국어 사전을 기준으로 번역 키 타입을 검사하고,
`pnpm i18n:check`는 두 사전의 누락·추가 키, 빈 번역문, 치환 변수 이름 차이를 검사합니다.
여행 날짜는 날짜 전용 값과 UTC 기준 `Intl.DateTimeFormat`, 인원·숫자는 `Intl.NumberFormat`을
사용합니다. 브랜드 보기에서 한영 날짜·숫자 표시를 비교할 수 있습니다.

## 테마와 선택 저장

프로필 → 설정에서 시스템·라이트·다크를 선택합니다. 기본값은 시스템이며 시스템 선택 중에는
OS 테마 변경을 추적합니다. 헤더의 해·달 버튼은 현재 화면과 반대되는 명시적 테마를 선택합니다.
`public/assets/theme.js`를 HTML head에서 먼저 실행하고 `html[data-theme]`의 공통 토큰을 적용합니다.
React는 같은 상태를 구독하므로 첫 화면과 앱 실행 후 테마를 따로 계산하지 않습니다.

선택은 같은 브라우저·출처의 `localStorage`에 `sayroam.theme.v1`, `sayroam.language.v1`로 저장하며
다른 탭의 변경·삭제도 반영합니다. 저장소 사용이 제한되면 현재 화면에서만 선택을 유지합니다.
계정·서버 간 동기화는 포함하지 않습니다. 테마·언어 설정은 업무 API 없이 동작합니다.

`?theme=dark|light|system`, `?lang=en|ko`는 저장하지 않는 일회성 미리보기 값으로 저장된 선택보다
우선합니다. UI에서 해당 설정을 고르면 저장하고 그 매개변수만 URL에서 제거하므로 다음 로드부터
사용자 선택이 적용됩니다. 설정 회귀 검사는 Node 내장 테스트로 실행하며 별도 테스트 프레임워크를
설치하지 않습니다.

## 화면 초안과 계약 범위

기본 주소는 홈(`/`)이며 여행(`/trips`)·동행(`/matching`)·내 방(`/rooms`)·브랜드(`/brand`)를
실제 URL로 이동합니다. 링크의 새 탭 열기, 브라우저 뒤로·앞으로 가기와 새로고침을 지원합니다.
기존 루트의 `?view=trips|matching|rooms|brand`는 해당 주소로 전환하고 나머지 설정·해시를 유지합니다.
현재 경로가 있으면 `view`보다 경로를 우선합니다. `?theme=dark`, `?lang=en`, `?modal=auth`는
계속 미리보기에 사용할 수 있습니다.

브랜드 페이지 상단의 **페이지 상태**에서 실제 화면과 같은 공용 `PageState`의 404·오류·로딩을
비교합니다. **404 전체 화면 보기**는 실제 없는 URL로 이동합니다. 일반적인 잘못된 화면 URL은
404 안내와 홈 이동을 제공하고, 화면 실행·모듈 로드 실패는 내부 오류 내용을 숨긴 안내와 다시 시도
동작을 제공합니다. 다시 시도는 페이지를 새로 불러옵니다. 페이지 모듈은 필요한 시점에 불러오며
첫 로드·화면 전환 중에는 문구와 함께 공용 스피너를 표시합니다.

이동 후 제목으로 포커스를 옮기고 화면 제목을 알립니다. Tab으로 보이는 본문 건너뛰기 링크를
제공하며, 모션 줄이기 설정에서는 스피너 회전을 생략하고 안내 문구를 유지합니다.
API·파일·Swagger·서버 및 인증 예약 경로는 앱의 404 화면으로 변환하지 않습니다.
인증과 보호 화면·로그인 복귀 처리는 이번 #56 범위에서 제외하고 마지막 #55에서 연결합니다.

로그인 모달은 Google·카카오·네이버의 공식 로고와 이메일 입력 UI를 제공합니다.
OAuth 이동·이메일 인증·계정 생성·비밀번호 찾기·계정 기억·여행 저장·채팅은 실행하지 않습니다.
설정 메뉴의 단축키는 macOS `⌘,`, 다른 환경 `Ctrl+,`이며 입력 중이나 다른 모달 안에서는 실행하지 않습니다.

화면은 API를 호출하지 않습니다. 후속 연결에서는 `FRONTEND.md`의 생성 타입 → API·오류 → 도메인 훅
→ 페이지 방향을 따릅니다. 요청·응답을 수기로 중복 선언하지 않습니다.

## API 접속 주소와 설정

브라우저는 개발·운영 모두 현재 화면과 같은 출처의 `/api/...`를 호출합니다.
개발 화면 주소는 `http://localhost:5173`이며 Vite가 `/api`를 `API_PROXY_TARGET`으로 전달합니다.
기본 대상은 `http://localhost:8080`입니다. `.env.example`을 `.env.local`로 복사해 실제 백엔드
주소를 설정하고 Vite를 다시 실행합니다. 이 값은 개발 프록시용이며 브라우저의 API 기본 주소가 아닙니다.
`localhost`와 `127.0.0.1`은 쿠키 저장 범위가 다르므로 화면 호스트를 일관되게 사용합니다.

운영은 HTTPS의 동일 출처에서 화면과 `/api`를 제공하며 Spring Boot 통합 JAR 또는 같은 출처의
리버스 프록시를 사용합니다. `/api` 응답을 SPA HTML로 대체하지 않습니다. 쿠키 속성은 서버가
설정하며 별도 브라우저용 Refresh Token 설정이나 비밀 키를 만들지 않습니다.
`OPENAPI_URL`은 계약 수신 도구 전용, `API_PROXY_TARGET`은 개발 서버 전용입니다.
`VITE_*`는 공개 번들 설정이므로 DB 비밀번호·JWT 서명 키·Refresh Token 등을 넣지 않습니다.
서버 비밀값은 백엔드 환경에서만 관리합니다.

## 공통 요청과 CSRF

일반 API는 `src/api/api.ts`의 `api.call(operationId, options)`로 호출합니다.
`schema.ts`의 생성 타입·경로표가 URL·HTTP 메서드·Bearer·CSRF 요구를 제공하므로 호출마다
정책을 선택하거나 전용 호출 함수를 작성하지 않습니다. 본문·경로·쿼리 입력과 반환 타입도
명세에서 가져옵니다. 전송·직렬화는 공통 실행기와 `openapi-fetch`가 담당합니다.

`src/api/csrf.ts`는 현재 `XSRF-TOKEN` 쿠키를 읽고, 없으면 `GET /api/auth/csrf`를 호출합니다.
같은 페이지의 동시 호출은 준비 요청 하나를 기다리며, 완료 후 쿠키 값을 `X-XSRF-TOKEN` 헤더로
전달합니다. 쿠키를 읽을 수 없으면 원래 변경 요청을 보내지 않습니다. 쿠키는 브라우저가 전송하고
Refresh 쿠키는 JavaScript로 읽지 않습니다. 개발 화면의 호스트를 바꾸면 쿠키도 별도로 관리됩니다.

`src/api/transport.ts`는 실제 전송·응답·오류 처리를 담당하며 CSRF 준비 GET도 이 계층을 사용합니다.
403과 네트워크 실패 후 원래 변경 요청을 자동 반복하지 않습니다. 한 호출의 취소는 공유 준비를
중단하지 않으며, 준비 완료 후 취소된 변경 요청은 전송되지 않습니다.
회귀 검사는 `pnpm test:csrf`로 실행합니다. 현재 화면 초안에는 아직 API를 연결하지 않았습니다.

## 로그인과 Access Token 기반

`src/api/auth/authSession.ts`의 `signIn(input, { signal })`이 로그인 → 메모리 토큰 저장 →
`GET /api/users/me` 조회를 조합하고, 확인한 회원 정보를 반환합니다. 입력과 응답은 생성 타입을
재사용하며 로그인·회원 응답의 실제 필드도 검사합니다. 화면 연결 시 도메인 훅에서 호출합니다.

`authApi.ts`·`userApi.ts`는 로그인·갱신·로그아웃·회원 조회의 기존 호출 이름을 제공하고
operationId와 입력만 전달합니다. 실제 검증과 부가 처리는 `api.ts`의 엔드포인트 확장에 등록합니다.
`tokenResponse.ts`는 토큰 필수값·Bearer 형식·양수인 안전 정수 유효기간을 검사하고,
`userResponse.ts`는 회원 정보의 필수 필드·nullable·역할·양수인 안전 정수 ID를 검사합니다.
모든 API에 별도 검증기를 생성하지 않으며 핵심 응답의 실제 구조를 명시적으로 검증합니다.
`tokenStore.ts`는 모듈 메모리만 사용합니다. 토큰을 localStorage·sessionStorage·직접 만든 쿠키에
저장하지 않습니다. 만료 시각은 참고값이며 최종 인증 유효성은 서버가 검사합니다.
새로고침하면 메모리 토큰은 사라집니다.

Bearer·CSRF 요구는 OpenAPI의 `security`에서 경로표로 생성됩니다. Refresh Token은 브라우저의
HttpOnly 쿠키 전송에 맡깁니다. 쿠키 변경처럼 명세만으로 결정하지 않는 규칙은 로그인·갱신·
로그아웃의 확장 설정인 `cookieAction`으로 지정하며 파싱·검증·후처리까지 탭 간 잠금으로 묶습니다.
도메인 API가 Access 헤더·CSRF·갱신을 직접 구현하지 않으며 전송 계층도 도메인 API를 역참조하지 않습니다.

새 로그인은 기존 메모리 인증을 정리하고 같은 탭의 중복 로그인은
`authentication-in-progress`로 거절합니다. 계정 변경·로그아웃은 인증 버전을 변경하고,
동일 로그인에서 Access Token만 갱신할 때는 버전을 유지합니다. 이전 계정의 응답이 현재 토큰을
덮거나 지우지 않으며 응답 검증이 끝난 후에도 인증 버전을 확인합니다.
로그인 성공 후 회원 조회만 일시 실패하면 토큰은 유지하며 로그인 POST를 자동 반복하지 않습니다.

### 파일별 책임과 새 API 연결

| 위치                                                                       | 소유하는 동작                                           |
| -------------------------------------------------------------------------- | ------------------------------------------------------- |
| `contracts/openapi.json` → `scripts/openapi.mts`                           | 명세에서 타입·경로표 생성                               |
| `src/api/generated/schema.ts`                                              | 타입과 `apiRoutes` 경로표 한 파일                       |
| `src/api/api.ts`                                                           | 공개 API 인스턴스·엔드포인트별 확장·공유 갱신 연결      |
| `src/api/router.ts`                                                        | operationId 조회·직렬화·성공 상태와 본문 계약·확장 실행 |
| `src/api/http.ts`, `transport.ts`, `csrf.ts`                               | Bearer·CSRF·조회 재시도·취소·실제 전송·오류 정규화      |
| `src/api/auth/tokenResponse.ts`, `users/userResponse.ts`                   | 핵심 응답 검증                                          |
| `src/api/auth/tokenStore.ts`, `refreshSession.ts`, `cookieCoordination.ts` | 메모리 토큰·공유 갱신·탭 간 쿠키 요청 직렬화            |
| `src/api/auth/authSession.ts`                                              | 로그인·복원·로그아웃의 상태 변경 순서                   |
| 도메인 API·훅 → 페이지                                                     | 업무 조합·오류 해석·요청 수명주기와 화면 연결           |

새 API는 명세 갱신·재생성 후 `api.call("operationId", options)`로 바로 호출합니다.
추가 검증은 `parseResponse`, 전후처리는 `wrap(next, context)`로 해당 엔드포인트에만 등록합니다.
이 확장도 공통 취소·인증 버전·쿠키 잠금 범위에서 실행됩니다. 일반 API에는 별도 래퍼가 필요하지 않습니다.
여러 요청과 상태 변경을 묶을 때만 세션 조합 모듈을 추가합니다.
ESLint는 직접 클라이언트 생성·경로표·실행기·전송 계층의 직접 사용과 리터럴 동적 import·직접
fetch 우회를 검사합니다. 변수·별칭으로 숨긴 모든 호출까지 보장하는 검사는 아닙니다.

### Refresh와 로그아웃

```text
보호된 조회 → 401
  → 같은 로그인에서 이미 갱신했으면 새 Access Token 재사용
  → 아니면 refreshSession.ts의 Promise 하나를 공유
    → refreshApi() → 쿠키 요청 잠금 → CSRF 준비 → POST /api/auth/refresh
    → 토큰 응답 검증 → 메모리 Access Token 교체
  → 원래 조회를 한 번 재전송
```

자동 재전송은 GET·HEAD·OPTIONS에만 적용합니다. 변경 요청의 401은 현재 토큰을 정리하고
그대로 전달하며 Refresh나 원래 변경 요청을 반복하지 않습니다. 403·네트워크·5xx도 자동 갱신·
재전송하지 않습니다. 갱신 후 조회도 401이면 메모리를 정리하고 종료합니다. 이전 토큰의 늦은
401은 이미 교체된 토큰을 지우거나 갱신을 다시 실행하지 않습니다.

Refresh의 401·403·서버 장애·네트워크 실패·잘못된 응답은 메모리 인증을 정리하고 원래 오류를
전달합니다. 서버가 쿠키를 교체했을 수 있으므로 Refresh POST를 자동 반복하지 않습니다.
그 뒤 토큰 없는 보호 요청은 `authentication-required`로 전송 전에 거절합니다.
화면은 재로그인을 안내하고 무조건적인 갱신 재시도나 반복적인 복원 호출을 만들지 않습니다.

`restoreSession({ signal })`은 메모리에 토큰이 없을 때 공유 갱신을 거쳐 내 정보를 조회합니다.
앱 시작 시 도메인 훅에서 호출할 기반 함수이며 현재 정적 화면에는 연결하지 않았습니다.
한 화면이 취소돼도 공유 갱신은 완료하고 그 화면의 후속 조회·결과 전달만 중단합니다.
취소한 호출의 대기는 공유 작업 완료를 기다리지 않고 즉시 종료합니다. CSRF 준비와 응답
검증을 기다리는 동안의 취소도 같은 규칙을 따르며, `src/api/abort.ts`가 호출별 대기를 정리합니다.

`signOut()`은 메모리와 진행 중 요청을 먼저 무효화한 뒤 `logoutApi()`를 한 번 호출합니다.
서버 실패를 성공으로 바꾸지 않으며 쿠키 폐기 결과가 불확실하면 오류를 그대로 안내해야 합니다.
`clearAccessToken()`만 호출하는 것은 서버 로그아웃이 아닙니다. 회원 정보·보호된 서버 상태는
향후 도메인 훅에서 인증 무효화와 함께 정리하며 별도 회원 전역 저장소는 아직 만들지 않았습니다.

같은 출처의 여러 탭은 Web Locks의 `sayroam-auth-cookie` 잠금으로 로그인·Refresh·로그아웃
요청을 순서대로 전송합니다. 탭마다 필요한 Access Token은 각자 메모리에 보관하며 갱신 쿠키는
앞 요청이 교체한 최신 값을 브라우저가 다음 요청에 전송합니다. 로그인·로그아웃·갱신 실패는
BroadcastChannel로 다른 탭의 메모리 인증도 무효화하며 토큰 값을 전송하지 않습니다.
`sayroam.auth.cookie-state.v1`에는 비밀값이 아닌 작업 세대·자동 복원 차단 여부만 저장합니다.
갱신 실패·로그아웃 시 잠금 해제 전에 차단 표식을 남겨 대기 중인 다른 탭과 새로고침 후의
자동 복원도 중단합니다. 명시적으로 새 로그인에 성공하면 해제합니다. Access·Refresh Token은
이 표식에 포함하지 않습니다. 이 공유 저장소를 사용할 수 없으면 인증 쿠키 요청을 거절합니다.
HTTPS 또는 localhost의 Web Locks 지원 브라우저가 필요합니다. 잠금을 사용할 수 없으면
`authentication-unavailable`로 인증 쿠키 요청을 거절하며 잠금 없이 동시에 전송하지 않습니다.

회귀 검사는 `pnpm test:auth`, `pnpm test:csrf`, `pnpm test:api`로 실행합니다.
자동 검사는 HTTP 응답을 통제해 동시 401·늦은 응답·갱신 실패·취소·로그아웃을 검증합니다.
실제 계정의 로그인·Redis Refresh 교체·재사용 거절은 별도의 백엔드 연동 검증으로 구분합니다.
로그인·회원가입 화면 연결은 #55의 범위에 포함하지 않습니다.

## OpenAPI 계약 갱신

`contracts/openapi.json`에서 `src/api/generated/schema.ts` 한 파일을 생성합니다.
`openapi-typescript`의 타입에 operationId별 경로·메서드·인증 요구·성공 응답 정보를 담은
`apiRoutes`를 함께 기록합니다. API별 함수나 SDK 런타임·선언 파일을 별도로 생성하지 않습니다.

```ts
import { api } from "@/api/api";

const user = await api.call("me", { signal });
const room = await api.call("create", { body: { name: "여행" } });
```

필요한 엔드포인트만 `src/api/api.ts`에서 확장합니다.

```ts
extensions: {
    login: { cookieAction: "login", parseResponse: validateToken },
    refresh: { cookieAction: "refresh", parseResponse: validateToken },
    logout: { cookieAction: "logout" },
    me: { parseResponse: validateCurrentUser },
}
```

`wrap(next, context)`를 등록하면 호출 전후에 부가 처리를 실행할 수 있습니다.
입력·응답 타입은 명세와 동기화되지만 실제 DTO 구조 검증은 별개이므로 핵심 응답의 검증을 유지합니다.
경로표는 성공 상태·본문 유무도 제공하며 예상하지 않은 성공 응답은 `invalid-response`로 구분합니다.
현재 생성기는 JSON 응답과 Bearer·XSRF 헤더·Refresh 쿠키 계약을 지원합니다. 고유한 operationId가
없거나 서로 다른 인증 대안·지원하지 않는 인증 방식이 있으면 자동 생성 단계에서 실패합니다.

`pnpm api:generate`는 저장 명세로 타입·경로표를 재생성하고 `pnpm api:check`는 저장본과
재계산한 결과를 비교합니다. check는 결과물을 수정하지 않으며 일반 빌드에는 실행 중인
백엔드가 필요하지 않습니다.

백엔드의 기존 `exportOpenApi` 명령으로 실행 중인 local 서버에서 JSON을 받아 검증한 뒤,
명세와 타입을 함께 저장합니다. 아래 명령은 모두 `frontend/`에서 실행합니다.

```bash
pnpm api:pull
# 주소를 명시하거나 환경변수로 설정
pnpm api:pull --url http://127.0.0.1:18080/v3/api-docs
OPENAPI_URL='http://[::1]:8080/v3/api-docs' pnpm api:pull
# 백엔드가 이미 내보낸 파일을 사용하는 경우
pnpm api:pull --file ../backend/build/openapi/openapi.json
```

주소 우선순위는 `--url` → 셸의 `OPENAPI_URL` → `.env.local`의 `OPENAPI_URL` →
`http://localhost:8080/v3/api-docs`입니다. 개인 서버 주소는 `.env.example`을 `.env.local`로 복사해
설정합니다. IPv4/IPv6에서 서로 다른 프로세스가 8080 포트를 사용하면 실제 Spring 서버가 응답하는
주소(예: `http://[::1]:8080/v3/api-docs`)를 지정합니다. `.env.local`은 Git에서 제외합니다.
주소는 빌드용 설정이며 브라우저 환경변수에 넣지 않습니다. 수신은 Java/Gradle과 실행 중인 백엔드가
필요하고, `--file`은 해당 파일만 사용합니다. HTTP·연결·JSON·스키마·참조 오류는 실패로 처리하며
검증·타입 생성을 마치기 전에는 기존 프론트 계약 파일을 교체하지 않습니다.
Windows에서는 `JAVA_HOME/bin/java.exe` 또는 PATH의 `java.exe`로 같은 Gradle Wrapper JAR를
셸 없이 실행합니다. macOS·Linux에서는 기존 `gradlew`를 사용합니다.

| 파일                          | 역할                                          |
| ----------------------------- | --------------------------------------------- |
| `contracts/openapi.json`      | Git으로 관리하는 백엔드 명세 스냅샷           |
| `src/api/generated/schema.ts` | 생성된 경로·요청·응답·오류 타입과 `apiRoutes` |
| `scripts/openapi.mts`         | 명세 수신·검증·생성 및 일치 여부 확인         |

```bash
pnpm api:generate # 저장된 명세로 재생성: 백엔드·네트워크 불필요
pnpm api:check    # 스냅샷 형식과 생성 타입·경로표의 일치 검사, 파일을 수정하지 않음
pnpm build       # 저장된 계약으로 빌드: 백엔드 불필요
git diff -- contracts/openapi.json src/api/generated/schema.ts
```

백엔드 API 변경 시 `api:pull` → diff 검토 → `api:check` → 빌드 후 명세와 타입을 같이 커밋합니다.
`api:check`는 저장된 명세와 생성 결과만 비교합니다. 실행 중인 서버와의 차이는 다시 수신한 뒤
Git diff로 확인합니다. 오프라인 재생성을 보장하기 위해 외부 `$ref`는 받지 않습니다.
키를 정렬하고 저장소 포맷으로 생성하며 `schema.ts`는 직접 수정하지 않습니다.

```ts
import type { components, paths } from "@/api/generated/schema";

type LoginRequest = paths["/api/auth/login"]["post"]["requestBody"]["content"]["application/json"];
type LoginResponse =
    paths["/api/auth/login"]["post"]["responses"][200]["content"]["application/json"];
type ApiProblem = components["schemas"]["ApiProblem"];
type KnownErrorCode = components["schemas"]["ApiErrorCode"];
```

상태 코드별 응답·헤더와 `application/problem+json`, 오류의 `code`·필드별 `errors`·선택적인
`retryAfterSeconds`를 그대로 가져옵니다. `ApiErrorCode`는 백엔드 오류 enum에서 생성한 알려진 코드이고,
`ApiProblem.code`는 Spring 기본 요청 오류와 새 코드도 수용하는 문자열입니다. 알 수 없는 코드는
공통 오류 처리에서 대체 메시지로 다룹니다(#55). 응답 필수값·선택값·nullable과 enum을 유지하며,
문자열 길이·패턴·이메일 형식 등은 명세에 남습니다. TypeScript 타입 자체가 런타임 입력 검증을
수행하지는 않습니다.

프로젝트 설정과 화면 초안은 [#60](https://github.com/Daebangg/sayroam/issues/60)으로 통합했습니다.
OpenAPI 명세·타입 연동은 [#53](https://github.com/Daebangg/sayroam/issues/53),
백엔드 빌드의 정적 파일 포함은 [#54](https://github.com/Daebangg/sayroam/issues/54),
공통 API·인증 처리는 [#55](https://github.com/Daebangg/sayroam/issues/55),
화면 라우팅은 [#56](https://github.com/Daebangg/sayroam/issues/56),
CI 통합은 [#57](https://github.com/Daebangg/sayroam/issues/57),
공용 UI·쇼케이스·정합성 자동화는 [#58](https://github.com/Daebangg/sayroam/issues/58),
다국어·테마 전환은 [#59](https://github.com/Daebangg/sayroam/issues/59)에서 진행합니다.
