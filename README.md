# sayroam

- [공통 에이전트 작업 지침 및 스킬](AGENTS.md)

- `backend/`: Spring Boot 백엔드 (Java 21, Gradle)
- `frontend/`: React·TypeScript 프론트엔드 (Vite, pnpm)
- `docker/`: 로컬 MySQL·Redis 개발 환경

- [프론트엔드 설치·실행·검증 방법](frontend/README.md)
- [로컬 앱 환경변수 및 실행 방법](backend/README.md)
- [화면을 포함한 배포용 JAR 빌드](backend/README.md#프론트엔드를-포함한-배포용-빌드)
- [Docker 로컬 개발 환경 및 실행 명령](docker/README.md)

## 루트 개발 명령

저장소 루트의 `Makefile`은 기존 Gradle Wrapper와 `frontend/`의 pnpm 명령을 호출합니다.
macOS·Linux의 GNU Make 3.81 이상을 사용합니다. 기본 `make` 또는 `make help`는 도움말만 표시합니다.
다른 위치에서는 `make -C /저장소/경로 <명령>`으로 실행합니다.

| 명령 | 용도 |
| --- | --- |
| `make env-init` | 없는 개인 설정 파일만 예시에서 복사, 기존 파일 보존 |
| `make frontend-install` | `pnpm install --frozen-lockfile` |
| `make backend-run` | `bootRun --args='--spring.profiles.active=local'` |
| `make frontend-run` | Vite 개발 서버 (`pnpm dev`) |
| `make backend-build` | 백엔드 빌드·테스트 (`build`), Node.js 불필요 |
| `make frontend-build` | 프론트엔드 빌드 (`pnpm build`), 결과: `frontend/dist/` |
| `make build` | `clean build -PincludeFrontend`, 프론트엔드를 포함한 배포용 JAR |
| `make backend-test` | 백엔드 테스트 (`test`) |
| `make frontend-check` | 프론트엔드 검증 (`pnpm check`) |
| `make check` | 백엔드 테스트와 프론트엔드 검증 |
| `make docker-up` | MySQL·Redis 기동 및 healthy 대기 (`dockerInit`) |
| `make docker-status` / `make docker-logs` | 컨테이너 상태 / 최근 로그 조회 |
| `make docker-stop` / `make docker-down` | 중지 / 컨테이너·네트워크 제거, MySQL 볼륨 보존 |
| `make db-info` / `make db-migrate` / `make db-validate` | 마이그레이션 상태 / 미적용 변경 적용 / 이력 검증 |

백엔드 명령에는 JDK 21이 필요합니다. Docker 명령도 기존 Gradle 태스크를 사용하므로 JDK가 필요합니다.
프론트엔드 명령에는 [고정 Node.js·pnpm 버전](frontend/README.md#설치)이 필요합니다.
백엔드 테스트를 포함하는 `backend-build`, `backend-test`, `check`, `build`는 실행 중인 Docker 엔진과
Testcontainers의 임시 MySQL·Redis를 사용하며 개인 개발 DB에 마이그레이션하지 않습니다.
개별 프론트 명령은 먼저 `frontend-install`을 실행합니다. 통합 `build`는 프론트 의존성도 설치하며,
기존 백엔드 빌드 결과를 정리한 뒤 `backend/build/libs/`에 화면을 포함한 실행용 JAR를 생성합니다.

### 처음 실행 순서

1. JDK 21, Docker·Compose, GNU Make와 프론트엔드의 고정 Node.js·pnpm을 설치합니다.
   macOS에서 Java 버전 선택이 필요하면 `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`을 실행합니다.
2. 루트에서 `make env-init`을 실행합니다. `docker/.env`, `backend/config/application-local.properties`,
   `frontend/.env.local`을 생성합니다. 기존 파일과 심볼릭 링크는 덮어쓰지 않습니다.
3. Docker와 백엔드의 DB·Redis 접속 값을 맞춥니다. `openssl rand -base64 32`로 생성한 서명 키를
   백엔드 개인 설정의 `app.auth.access-token.secret-base64`에 입력합니다.
   필요하면 프론트 개인 설정의 `API_PROXY_TARGET`을 백엔드 주소에 맞춥니다.
   SMTP·소셜 로그인 등 외부 연동은 [백엔드 설정 안내](backend/README.md)를 따릅니다.
4. 아래 명령을 순서대로 실행합니다.

```bash
make frontend-install
make docker-up
make db-info
make db-migrate
make db-validate
make backend-run
```

별도 터미널의 저장소 루트에서 `make frontend-run`을 실행한 뒤 `http://localhost:5173`을 엽니다.
백엔드 기본 주소는 `http://localhost:8080`입니다. 두 개발 서버는 각각 `Ctrl+C`로 종료합니다.
Docker는 `make docker-stop`으로 중지하거나 `make docker-down`으로 컨테이너·네트워크를 제거합니다.
두 명령 모두 MySQL 데이터를 보존하며, Redis의 임시 인증 데이터는 재시작 시 사라집니다.

`env-init`은 템플릿 복사만 수행하며 서명 키 생성이나 접속 정보 동기화는 하지 않습니다.
앱 실행·빌드·종료는 개발 DB를 자동 기동하거나 마이그레이션하지 않습니다.
DB 변경은 `make db-migrate`로 명시적으로 실행합니다. 실제 비밀값은 개인 설정에만 저장합니다.
필수 도구나 설정이 없거나 하위 명령이 실패하면 make도 실패합니다. Gradle·pnpm·앱의 오류 안내를
확인하고 설정을 수정합니다. 버전과 접속 정보의 검증은 기존 도구가 담당합니다.

### 실행 옵션

기존 환경변수와 설정 우선순위를 유지합니다. 예를 들어 백엔드 포트를 바꿀 때는 다음처럼 실행하고,
프론트 `API_PROXY_TARGET`도 같은 주소로 수정합니다.

```bash
SERVER_PORT=18080 make backend-run
make frontend-run FRONTEND_ARGS='--port 5174'
make backend-test GRADLE_ARGS="--tests '*RoomServiceTest'"
make backend-run APP_ARGS='--spring.profiles.active=local --server.port=18080'
```

`GRADLE_ARGS`는 Gradle 명령 뒤에, `APP_ARGS`는 `bootRun --args`에,
`FRONTEND_ARGS`는 Vite 개발 서버 명령 뒤에 전달합니다. 값에 비밀값을 넣지 않습니다.
프론트 포트 변경 시 소셜 로그인 복귀 주소 등 관련 설정도 맞춥니다.

## 변경 범위별 CI

`dev`·`main` 대상 PR과 `dev` push에서 변경 파일에 따라 백엔드·프론트엔드 검증을 각각 실행합니다.

| 변경 범위 | 실행할 CI |
| --- | --- |
| `backend/**` 코드·설정 | Backend CI: `./gradlew build` |
| `frontend/**` 코드·설정·정적 자원 | Frontend CI: 잠금 파일 설치 → `pnpm check` → `pnpm build` |
| 양쪽 코드·설정 | Backend CI와 Frontend CI |
| Markdown(`*.md`), `backend/docs/**`, `frontend/notes/**`만 변경 | 두 CI 모두 생략 |
| 각 CI의 workflow 파일 | 해당 CI |

루트 문서와 `docker/**` 변경은 두 CI의 실행 대상에 포함하지 않습니다.
현재 자동 검증은 백엔드·프론트엔드 두 범위이며, 통합 JAR·Docker·배포·문서 전용 검증은 별도 후속 범위입니다.
