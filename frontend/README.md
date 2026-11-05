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
의존성을 추가·갱신할 때는 `pnpm install`로 이 디렉터리의 `pnpm-lock.yaml`도 함께 갱신합니다.
`pnpm-workspace.yaml`은 이 앱의 pnpm 설치 정책 설정이며 루트 JavaScript workspace를 만들지 않습니다.

## 개발 및 검증

아래 명령은 모두 `frontend/`에서 실행합니다.

| 명령                | 용도                                               |
| ------------------- | -------------------------------------------------- |
| `pnpm dev`          | 개발 서버 실행: `http://localhost:5173`            |
| `pnpm typecheck`    | 앱과 Vite 설정의 TypeScript 검사                   |
| `pnpm lint`         | ESLint 검사, 경고도 실패로 처리                    |
| `pnpm format:check` | 공통 설정·프론트엔드 포맷 검사                     |
| `pnpm format`       | 공통 설정·프론트엔드 포맷 적용                     |
| `pnpm check`        | 타입·ESLint·포맷 검사 순서대로 실행                |
| `pnpm build`        | 타입 검사 후 배포용 파일을 `frontend/dist/`에 생성 |
| `pnpm preview`      | 빌드 결과 미리보기: `http://localhost:4173`        |

개발 서버와 미리보기는 지정 포트를 사용합니다. 포트가 사용 중이면 다른 포트로 자동 이동하지 않고
실패하므로 기존 프로세스를 확인하거나 `pnpm dev --port 5174`처럼 포트를 지정합니다.
`pnpm preview`는 먼저 `pnpm build`를 실행해야 하며 운영 서버로 사용하지 않습니다.
`frontend/` 안에서는 `pnpm dev`, `pnpm typecheck`, `pnpm lint`, `pnpm build`, `pnpm preview`를
직접 실행할 수 있습니다. 저장소 루트에는 프론트엔드 명령이나 Node 의존성을 두지 않습니다.

## 구성 및 범위

- `src/main.tsx`: React 시작점, 개발 중 StrictMode 적용
- `src/App.tsx`: 화면·테마·모달 미리보기 상태와 화면 조합
- `src/pages/`: 홈·여행·동행·내 방·브랜드 보기의 정적 초안
- `src/components/ui/`: 공용 버튼·입력·드롭다운·태그·모달·툴팁·단축키 표시
- `src/components/features/`: 로그인 모달·프로필 메뉴·설정 미리보기
- `src/components/layout/`: 데스크톱 상단·모바일 하단 메뉴와 앱 셸
- `src/data/preview.ts`: 업무 API와 연결하지 않는 예시 데이터
- `public/brand/`: SVG·PNG 로고·심볼·워드마크·앱 아이콘과 공식 로그인 제공자 자산
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

기본 언어와 대체 언어는 한국어(`ko`)이며 영어(`en`) 리소스를 함께 제공합니다.
브라우저 언어와 무관하게 한국어로 시작하고 컴포넌트는 `useTranslation()`의 `t()`를 사용합니다.
새 문구는 두 JSON 사전에 같은 키로 추가합니다. 한국어 사전을 기준으로 번역 키 타입을 검사하며
영어 사전의 필수 키가 빠지면 타입 검사에서 실패합니다.

언어는 공용 i18n 인스턴스의 `changeLanguage("en")` 또는 `changeLanguage("ko")`로 바꿀 수 있습니다.
변경 시 React 문구·HTML `lang`·문서 설명을 함께 갱신합니다. 현재 헤더·설정의 언어 및 테마 전환은
미리보기용 로컬 상태이며 선택 저장·날짜/숫자 지역화·운영 설정 연동은 후속 #59에서 진행합니다.

## 화면 초안과 계약 범위

기본 주소는 한국어 홈입니다. `?view=trips`, `?view=matching`, `?view=rooms`, `?view=brand`,
`?theme=dark`, `?lang=en`, `?modal=auth`를 조합해 정적 화면을 미리 볼 수 있습니다.
상단 메뉴·모바일 메뉴는 미리보기 화면만 전환하며 실제 라우터는 아직 연결하지 않습니다.
브랜드 보기에서 공용 UI의 기본·비활성·로딩 상태와 SVG·PNG 자산을 확인할 수 있습니다.

로그인 모달은 Google·카카오·네이버의 공식 로고와 이메일 입력 UI를 제공합니다.
OAuth 이동·이메일 인증·계정 생성·비밀번호 찾기·계정 기억·여행 저장·채팅은 실행하지 않습니다.
설정 메뉴의 단축키는 macOS `⌘,`, 다른 환경 `Ctrl+,`이며 입력 중이나 다른 모달 안에서는 실행하지 않습니다.

화면은 API를 호출하지 않습니다. 후속 연결에서는 `FRONTEND.md`의 생성 타입 → API·오류 → 도메인 훅
→ 페이지 방향을 따릅니다. 요청·응답을 수기로 중복 선언하지 않습니다.

## OpenAPI 계약 갱신

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

주소 우선순위는 `--url` → `OPENAPI_URL` → `http://localhost:8080/v3/api-docs`입니다.
주소는 빌드용 설정이며 브라우저 환경변수에 넣지 않습니다. 수신은 Java/Gradle과 실행 중인 백엔드가
필요하고, `--file`은 해당 파일만 사용합니다. HTTP·연결·JSON·스키마·참조 오류는 실패로 처리하며
검증·타입 생성을 마치기 전에는 기존 프론트 계약 파일을 교체하지 않습니다.
Windows에서는 `JAVA_HOME/bin/java.exe` 또는 PATH의 `java.exe`로 같은 Gradle Wrapper JAR를
셸 없이 실행합니다. macOS·Linux에서는 기존 `gradlew`를 사용합니다.

| 파일                          | 역할                                                   |
| ----------------------------- | ------------------------------------------------------ |
| `contracts/openapi.json`      | Git으로 관리하는 백엔드 명세 스냅샷                    |
| `src/api/generated/schema.ts` | `openapi-typescript`로 생성한 경로·요청·응답·오류 타입 |
| `scripts/openapi.mts`         | 명세 수신·검증·생성 및 일치 여부 확인                  |

```bash
pnpm api:generate # 저장된 명세로 재생성: 백엔드·네트워크 불필요
pnpm api:check    # 스냅샷 형식과 생성 타입의 일치 검사, 파일을 수정하지 않음
pnpm build       # 저장된 계약으로 빌드: 백엔드 불필요
git diff -- contracts/openapi.json src/api/generated/schema.ts
```

백엔드 API 변경 시 `api:pull` → diff 검토 → `api:check` → 빌드 후 명세와 타입을 같이 커밋합니다.
`api:check`는 저장된 명세와 생성 결과만 비교합니다. 실행 중인 서버와의 차이는 다시 수신한 뒤
Git diff로 확인합니다. 오프라인 재생성을 보장하기 위해 외부 `$ref`는 받지 않습니다.
키를 정렬하고 저장소 포맷으로 생성하며, 생성 파일은 직접 수정하거나 수동 린트 수정을 하지 않습니다.
앱은 TypeScript 6을 사용하고, 생성기 내부는 해당 도구의 peer 요구에 맞는 TypeScript 5.9로 분리합니다.

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
