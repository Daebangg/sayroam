# 로컬 앱 개발 환경

Spring Boot의 기본 프로필 설정 파일을 사용합니다. 앱 전용 `.env`나 별도 import는 사용하지 않습니다.
실행 디렉터리인 `backend/` 아래 `config/application-local.properties`를 `local` 프로필에서 자동으로 읽습니다.
이 파일은 소스 리소스 밖에 있으므로 앱 JAR에 포함되지 않습니다.

## 개인 설정 준비

저장소 루트에서 실행합니다. 기존 파일은 덮어쓰지 않습니다.

```bash
cp -n backend/config/application-local.properties.example backend/config/application-local.properties
```

복사한 파일의 다음 값을 실제 접속할 DB에 맞게 수정합니다.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/sayroam
spring.datasource.username=sayroam
spring.datasource.password=sayroam-local
```

Docker DB를 사용한다면 URL의 포트·DB명과 계정 정보를 `docker/.env`에 맞춥니다.
두 설정은 별도로 관리하며 자동 동기화하지 않습니다. 앱에는 root 비밀번호가 필요하지 않습니다.

## 공유 및 Git 관리

| 파일 | 용도 | Git 관리 |
| --- | --- | --- |
| `src/main/resources/application.properties` | 공통 앱 설정, 비밀값 제외 | 포함 |
| `config/application-local.properties.example` | 로컬 설정 키와 개발용 예시 | 포함 |
| `config/application-local.properties` | 개인의 실제 접속 정보 | 제외 |

예시 파일에는 실제 서버 주소·계정·비밀번호를 넣지 않습니다. 새 설정 키가 생기면 예시 파일과 이 문서를 갱신하고,
팀원은 자신의 실제 파일에 해당 키를 추가합니다. `.example` 파일은 Spring Boot가 자동으로 읽지 않습니다.
Properties는 `키=값`으로 작성하고 값에 따옴표를 붙이지 않습니다. 주석은 별도의 `#` 줄에 작성합니다.

## 실행

DB 준비 후 [마이그레이션 안내](gradle/README.md)에 따라 스키마를 적용하고 앱을 실행합니다.

```bash
./backend/gradlew -p backend flywayMigrate
./backend/gradlew -p backend bootRun --args='--spring.profiles.active=local'
```

IntelliJ Run Configuration:

- Active profiles: `local`
- Working directory: 저장소의 `backend/` 절대 경로
- Environment variables: 개인 설정 파일을 사용하면 별도 입력 불필요

환경변수 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`는
파일의 연결 설정보다 우선합니다. 운영에서는 `local` 프로필 없이 이 환경변수로 연결 정보를 주입합니다.
앱 실행은 Docker를 자동 기동하거나 스키마를 변경하지 않습니다.
테이블 생성·변경은 `flywayMigrate`로 실행하고 앱은 엔티티와 DB 매핑을 검증합니다.
빈 DB 또는 미적용 변경이 있으면 마이그레이션을 먼저 실행하세요.

## 엔티티 참조 규칙

다른 패키지의 엔티티는 객체 대신 `Long xxxId`로 참조합니다.
객체로 들고 있으면 한 도메인 코드에서 다른 도메인 엔티티의 상태가 경고 없이 바뀔 수 있고,
테스트에서 엔티티 하나를 만들 때 참조하는 엔티티까지 유효한 상태로 만들어야 하기 때문입니다.

```java
// 다른 패키지(user)의 엔티티: ID로 참조
@Column(name = "sender_id", nullable = false)
private Long senderId;

// 같은 패키지(room)의 엔티티: 객체 참조 허용
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "room_id", nullable = false)
private Room room;
```

- 같은 패키지 안의 객체 참조는 허용하되, 참조한 엔티티의 상태를 바꾸지 않습니다.
- 다른 도메인 객체가 필요하면 서비스에서 리포지토리로 조회합니다.
- 화면용 목록 조회는 조인 쿼리로 필요한 값만 DTO로 조회합니다.
- DB의 FK 제약은 마이그레이션에 그대로 유지합니다.

## 인증 접근 규칙 (기반 설정)

일반 API는 `Authorization: Bearer <Access Token>` 인증을 도입할 예정입니다.
현재는 접근 규칙만 구성했으며, 토큰 발급·검증과 실제 회원가입·로그인 API는 아직 없습니다.
따라서 현재 보호된 API에 임의의 Bearer 토큰을 전달해도 인증되지 않습니다.

| 요청 | 접근 규칙 |
| --- | --- |
| `POST /api/auth/signup`, `POST /api/auth/login` | 인증 없이 접근 가능, CSRF 검증은 적용 |
| `GET /swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs`, `/v3/api-docs/**`, `/v3/api-docs.yaml` | 인증 없이 조회 가능, 문서 기능은 기본적으로 local에서만 활성화 |
| `/api/admin`, `/api/admin/**` | `ROLE_ADMIN` 권한 필요 |
| 그 외 요청 | 인증 필요 |

인증 상태는 HTTP 세션에 저장하거나 기존 세션에서 복원하지 않습니다.
기본 로그인 화면, HTTP Basic, 기본 로그아웃과 원래 요청의 세션 저장도 비활성화했습니다.
내부 ERROR 디스패치만 허용하며, 외부에서 직접 `/error`로 접근하는 요청은 공개하지 않습니다.
인증되지 않은 일반 보호 요청은 401, 인증된 회원의 권한 부족은 403으로 처리합니다.
MVC 예외와 Security 필터 예외는 `application/problem+json` 형식과 `code` 필드를 공유합니다.

| 상황 | HTTP 상태 | code |
| --- | --- | --- |
| 인증 필요 | 401 | `UNAUTHENTICATED` |
| 권한 부족 | 403 | `ACCESS_DENIED` |
| CSRF 토큰 누락·오류 | 403 | `INVALID_CSRF_TOKEN` |
| 입력값 검증 실패 | 400 | `INVALID_INPUT` (`errors`에 field/reason 포함) |
| 예상하지 못한 오류 | 500 | `INTERNAL_SERVER_ERROR` (내부 정보는 로그에만 기록) |

Spring 기본 요청 오류에는 `METHOD_NOT_ALLOWED` 등 HTTP 상태 이름을 코드로 사용합니다.
`type`은 생략될 수 있으며, 생략 시 `about:blank`를 의미합니다.

```json
{
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authentication is required",
  "instance": "/api/users/me",
  "code": "UNAUTHENTICATED"
}
```

무상태 설정만으로 CSRF 방어가 불필요해지는 것은 아닙니다. 현재 CSRF 검증을 유지하고
CSRF 토큰 저장에 HTTP 세션 대신 쿠키를 사용합니다. 공개 POST 요청도 CSRF 토큰이 없거나
잘못되면 403입니다. 이 쿠키는 로그인 세션이나 Access Token을 저장하는 쿠키가 아닙니다.
Bearer 검증을 도입할 때 헤더로만 인증하는 경로의 제외 범위를 정하고, Refresh Token 등
쿠키를 사용하는 경로는 별도로 방어합니다. 프론트엔드의 CSRF 토큰 전달과 CORS 허용 출처는
실제 연동 단계에서 구성합니다.

접근 정책 테스트는 테스트 전용 API와 인증 정보를 사용합니다. JWT 검증 성공을 의미하지 않습니다.

```bash
./backend/gradlew -p backend test --tests '*SecurityConfigTests'
```


## Swagger / OpenAPI

`local` 프로필 실행 후 `http://localhost:8080/swagger-ui/index.html`에서 확인합니다.
JSON 명세는 `/v3/api-docs`, YAML 명세는 `/v3/api-docs.yaml`입니다.
공통 설정은 문서 기능을 끄고, 클래스패스의 `application-local.properties`에서 활성화합니다.
외부 `config/application-local.properties`에는 기존처럼 개인 DB 연결 정보만 두면 됩니다.

현재 업무 Controller는 없으므로 `paths`는 비어 있습니다. 공통 오류 스키마·응답 정의와
향후 사용할 `bearerAuth` 형식만 등록했습니다. Swagger의 Authorize 입력은 로그인 기능이
아니며, 실제 토큰 발급·검증은 #30에서 구현합니다. 각 API 구현 시 실제 상태 코드에 맞춰
공통 응답을 참조하고, 보호 API에는 `@SecurityRequirement(name = "bearerAuth")`를 지정합니다.
공개 API에는 인증 요구사항을 지정하지 않습니다.

실행 중인 앱에서 프론트엔드 전달용 파일을 생성합니다(서버를 자동 기동하지 않습니다).

```bash
./backend/gradlew -p backend exportOpenApi
# 별도 포트로 실행했다면
./backend/gradlew -p backend exportOpenApi -PapiDocsUrl=http://127.0.0.1:18080/v3/api-docs
```

결과는 `backend/build/openapi/openapi.json`입니다. `info.version`은 Gradle 프로젝트 버전
(`0.0.1-SNAPSHOT`)입니다. Gradle 빌드 정보 없이 IDE에서 실행하면 버전은 `dev`로 표시됩니다.
API 변경 시 버전을 관리하고 전달 파일과 소스 커밋을 함께 기록합니다.
명세의 서버 URL은 `/`로 고정하여 개발자별 호스트·포트가 계약 파일에 섞이지 않게 합니다.
프론트엔드는 이 파일을 타입 생성기의 입력으로 사용할 수 있습니다.

추가 테스트 작성·실행은 인증 기능 전체 구현 후 진행합니다. 현재 확인 범위는
컴파일, 비인증 요청의 401, CSRF 누락 요청의 403, 문서 HTTP 응답 및 파일 생성입니다.
인증된 사용자의 권한 부족, 입력 검증 API와 명세 일치 등은 후속 검증 대상입니다.

`localhost:8080` 내보내기가 404인데 브라우저에서는 정상이라면 IPv4/IPv6의 포트 점유가
다른지 확인하세요. 예를 들어 Spring 앱이 IPv6에서 실행 중이면 다음처럼 명시합니다.

```bash
./backend/gradlew -p backend exportOpenApi '-PapiDocsUrl=http://[::1]:8080/v3/api-docs'
```
