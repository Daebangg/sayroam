# 로컬 앱 개발 환경

Spring Boot의 기본 프로필 설정 파일을 사용합니다. 앱 전용 `.env`나 별도 import는 사용하지 않습니다.
실행 디렉터리인 `backend/` 아래 `config/application-local.properties`를 `local` 프로필에서 자동으로 읽습니다.
이 파일은 소스 리소스 밖에 있으므로 앱 JAR에 포함되지 않습니다.

## 개인 설정 준비

저장소 루트에서 실행합니다. 기존 파일은 덮어쓰지 않습니다.

```bash
cp -n backend/config/application-local.properties.example backend/config/application-local.properties
```

복사한 파일의 DB·Redis 값을 실제 접속 환경에 맞게 수정합니다. SMTP 계정이 있으면 아래 메일 설정도 추가합니다.
앱 시작에는 Access Token 서명 키도 필수입니다. `openssl rand -base64 32`로 생성한 값을
개인 파일의 `app.auth.access-token.secret-base64`에 입력합니다. 상세 정책은 [로그인 문서](docs/login.md)를 참고합니다.
로그인·토큰 갱신에는 MySQL과 Redis가 필요하며 [Refresh 쿠키·교체 정책](docs/refresh-token.md)을 함께 따릅니다.
현재 로그인 종료는 [로그아웃·Access Token 잔여 시간 정책](docs/logout.md)을 따릅니다.
비밀번호 변경·탈퇴 전 본인 확인은 [계정 관리용 이메일 인증·일회성 증명](docs/account-verification.md)을 따릅니다.
비밀번호 변경과 모든 기기의 기존 로그인 무효화는 [비밀번호 변경 문서](docs/password-change.md)를 따릅니다.
회원 탈퇴와 개인정보·로그인 수단 정리 및 재가입 기준은 [회원 탈퇴 문서](docs/account-deletion.md)를 따릅니다.
운영 Refresh 쿠키는 기본 Secure=true이고, HTTP `local` 프로필에서만 false를 사용합니다.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/sayroam
spring.datasource.username=sayroam
spring.datasource.password=sayroam-local
```

Docker DB를 사용한다면 URL의 포트·DB명과 계정 정보를 `docker/.env`에 맞춥니다.
두 설정은 별도로 관리하며 자동 동기화하지 않습니다. 앱에는 root 비밀번호가 필요하지 않습니다.

## 공유 및 Git 관리

| 파일                                          | 용도                       | Git 관리 |
|-----------------------------------------------|----------------------------|----------|
| `src/main/resources/application.properties`   | 공통 앱 설정, 비밀값 제외  | 포함     |
| `config/application-local.properties.example` | 로컬 설정 키와 개발용 예시 | 포함     |
| `config/application-local.properties`         | 개인의 실제 접속 정보      | 제외     |

예시 파일에는 실제 서버 주소·계정·비밀번호를 넣지 않습니다. 새 설정 키가 생기면 예시 파일과 이 문서를 갱신하고,
팀원은 자신의 실제 파일에 해당 키를 추가합니다. `.example` 파일은 Spring Boot가 자동으로 읽지 않습니다.
Properties는 `키=값`으로 작성하고 값에 따옴표를 붙이지 않습니다. 주석은 별도의 `#` 줄에 작성합니다.

## 실행

MySQL·Redis 준비 후 [마이그레이션 안내](gradle/README.md)에 따라 스키마를 적용하고 앱을 실행합니다.

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
운영에서는 `APP_AUTH_ACCESSTOKEN_SECRETBASE64`로 Access Token 서명 키를 주입할 수 있습니다.
테이블 생성·변경은 `flywayMigrate`로 실행하고 앱은 엔티티와 DB 매핑을 검증합니다.
빈 DB 또는 미적용 변경이 있으면 마이그레이션을 먼저 실행하세요.

## 프론트엔드를 포함한 배포용 빌드

Java 21과 [프론트엔드 개발 도구](../frontend/README.md#설치)(Node.js 24.18.0, pnpm 12.16.1)를
준비합니다. `pnpm`이 Gradle을 실행하는 터미널의 PATH에 있어야 합니다. IDE에서 실행할 때도
같은 PATH를 사용하도록 설정합니다. 아래 명령은 저장소 루트 기준입니다.

```bash
./backend/gradlew -p backend clean build -PincludeFrontend
# 테스트 없이 패키징만 다시 확인할 때
./backend/gradlew -p backend bootJar -PincludeFrontend
```

`includeFrontend`는 값 없이 지정하거나 `-PincludeFrontend=true`로 사용합니다.
`frontendInstall`이 `frontend/`에서 `pnpm install --frozen-lockfile`을 실행하고,
`frontendBuild`가 타입 검사와 Vite 빌드를 수행합니다. 실행 중인 백엔드나 OpenAPI 재수신은 필요하지
않으며 저장된 계약으로 빌드합니다. 프론트 결과는 `backend/build/generated/frontend/`에 생성하고
실행 가능한 `backend/build/libs/sayroam-0.0.1-SNAPSHOT.jar`의 `BOOT-INF/classes/static/`에 넣습니다.
`*-plain.jar`는 실행용 파일이 아닙니다. `src/main/resources/`나 `frontend/dist/`에는 복사하지 않습니다.

통합 빌드는 매번 잠금 파일 설치와 프론트 빌드를 실행합니다. 시작할 때 이전 프론트 출력과 실행용
JAR를 제거하므로 설치·타입 검사·Vite 실패 시 명령이 실패하고 이전 JAR도 배포 결과로 남지 않습니다.
`clean`은 생성된 프론트 결과도 제거하고, `clean` 없이 재빌드해도 삭제·이름 변경된 자원이 남지 않습니다.
`frontend/node_modules/`와 별도로 만든 `frontend/dist/`는 백엔드 `clean`의 정리 대상이 아닙니다.

개인 local 설정으로 실행하는 예입니다. 앞서 안내한 DB·Redis와 마이그레이션 준비가 필요합니다.

```bash
cd backend
java -jar build/libs/sayroam-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

`http://localhost:8080/`에서 화면을 열고 `/assets/**`, `/brand/**`, `/fonts/**`, `/images/**`에서
JS·CSS·브랜드·폰트·이미지를 같은 출처로 제공합니다. 프론트 서버나 실행 서버의 Node.js는 필요하지
않습니다. 운영은 `local` 없이 기존 환경변수·외부 설정으로 실행합니다.

화면의 GET·HEAD와 정적 자원만 공개하고 `/api/**`의 Bearer 인증·관리자 권한·auth CSRF 검사는
유지합니다. `/trips`, `/matching`, `/rooms`와 그 하위 경로, `/brand`는 직접 접속·새로고침 시 앱 셸을
제공합니다. 화면 경로 아래라도 파일 이름에 점이 있으면 404로 처리하며, 없는 정적 자원·API 오류를
HTML 200으로 바꾸지 않습니다. 앱 셸은 `Cache-Control: no-store`로 전달합니다.
루트는 Spring Boot의 기본 welcome page를 사용하고, SPA 화면 주소만 `/index.html`로 내부 forward합니다.
파일 위치 탐색과 HTML·정적 파일 전송은 Spring의 기본 리소스 핸들러가 담당합니다.

현재 UI의 화면 선택은 `/?view=trips` 등 기존 미리보기 쿼리를 사용합니다. 하위 화면 경로는 앱 셸을
여는 서버 측 기반이며, 경로별 화면 선택·클라이언트 이동·404 화면은 #56에서 연결하고 재검증합니다.
API 문서에는 화면 경로를 추가하지 않습니다. CI에서의 도구 설치·통합 빌드 검사는 #57 범위입니다.

일반 `test`, `build`, `bootJar`, `bootRun`은 `includeFrontend`를 생략하면 프론트 설치·빌드를 실행하지
않습니다. `local` 프로필에서는 IDE·`bootRun`도 이미 생성한 프론트 결과를 읽습니다.
통합 JAR를 만든 뒤 옵션 없이 `bootJar`를 실행하면 프론트가 빠진 백엔드 전용 JAR로 다시 패키징됩니다.

### IntelliJ·bootRun에서 화면 함께 보기

프론트를 빌드하면서 로컬 백엔드를 실행하려면 저장소 루트에서 다음 명령을 사용합니다.

```bash
./backend/gradlew -p backend bootRun -PincludeFrontend --args='--spring.profiles.active=local'
```

IntelliJ의 기존 `SayroamApplication` Run으로 실행할 때는 먼저 프론트 결과를 준비합니다.
통합 `build`·`bootJar`를 이미 실행했다면 다시 빌드할 필요가 없습니다.

```bash
./backend/gradlew -p backend frontendBuild
```

이후 IntelliJ에서 Active profiles=`local`, Working directory=`backend/`로 실행하면
`http://localhost:8080/`에서 화면을, `/swagger-ui/index.html`에서 Swagger를 함께 확인할 수 있습니다.
일반 `bootRun --args='--spring.profiles.active=local'`도 같은 결과를 읽습니다.

`application-local.properties`의 `spring.web.resources.static-locations`에
`file:./build/generated/frontend/`를 추가했습니다. 앱 셸과 JS·CSS·이미지는 같은 위치 설정을 사용하며,
JAR 안에 자원이 있으면 클래스패스 자원을 먼저 읽습니다. 운영 프로필은 클래스패스 자원만 사용합니다.
프론트를 다시 빌드한 뒤에는 로컬 백엔드를 재시작하지 않고 브라우저를 새로고침하면 됩니다.
로컬 리소스 체인 캐시는 꺼 두어 재빌드로 제거된 파일도 이전 참조를 사용하지 않고 404로 처리합니다.
`clean` 후에는 생성 결과가 지워지므로 `frontendBuild` 또는 `bootRun -PincludeFrontend`로 준비합니다.

`frontend/`의 `pnpm build`는 별도 미리보기용 `frontend/dist/`를 생성합니다. 백엔드와 함께 확인할 때는
위 Gradle 명령을 사용합니다. Vite 개발 서버가 필요한 프론트 작업은 기존 `pnpm dev`를 사용합니다.

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

## Redis 연결 및 인증 메일 발송 기반 (#28 1단계)

먼저 [Docker 환경](../docker/README.md)에 따라 `docker/.env`에 `REDIS_PORT=6379`를 추가하고
`dockerInit`으로 MySQL과 Redis를 준비합니다. 기존 개인 파일은 덮어쓰지 말고 새 키만 추가합니다.
앱은 Docker `.env`를 읽지 않으므로 외부 `config/application-local.properties`에도 별도로 설정합니다.

```properties
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379
spring.data.redis.database=0
```

`spring.data.redis.port`와 Docker의 `REDIS_PORT`를 맞춥니다. 기본 로컬 Redis에는 인증이 없습니다.
인증이 필요한 외부 서버에는 `spring.data.redis.username`, `spring.data.redis.password`를 추가합니다.
Boot가 이 설정으로 Lettuce `RedisConnectionFactory`를 구성하고 `RedisConfig`의 `StringRedisTemplate`이
이를 사용합니다. 키·값을 문자열로 처리하며 연결·명령 timeout은 각각 3초입니다.
Redis Repository와 CacheManager는 구성하지 않습니다. 앱 시작만으로 Redis 접속 성공이 보장되지는 않습니다.
시작 시 Redis 연결 검사는 없으므로 Redis가 꺼져 있어도 앱 시작이 가능하지만,
인증 메일 발급 요청의 코드 보관·발송 제한에는 Redis 서버가 필요합니다. 앱 내부 메모리 저장소로
자동 전환하지 않으며, 발급 요청은 Redis 장애 시 503으로 실패하며, 인메모리 저장소로 우회하지 않습니다.
Docker Redis는 프로세스 재시작 시 데이터가 사라지는 로컬 임시 저장소입니다.

SMTP 계정을 받은 경우 개인 파일에서 `app.mail.enabled=true`로 바꾸고 공급자 설정을 입력합니다.
예시는 465 포트의 인증 + SSL이며 STARTTLS는 끕니다. 587 포트의 STARTTLS를 쓰는 공급자는
SSL을 끄고 `spring.mail.properties[mail.smtp.starttls.enable]=true`와
`spring.mail.properties[mail.smtp.starttls.required]=true`로 설정합니다.
`spring.mail.username`은 로그인 계정이고 `app.mail.from`은 공급자가 허용하는 발신 주소입니다.
계정 비밀번호 대신 앱 비밀번호가 필요하면 공급자의 안내를 따릅니다. 실제 값은 커밋하지 않습니다.

- `spring.mail.host`, `spring.mail.port`: SMTP 서버와 포트
- `spring.mail.username`, `spring.mail.password`: SMTP 인증 정보
- `app.mail.from`: 모든 인증 메일의 From 주소 (SMTP 활성화 시 필수)
- 연결·읽기·쓰기 timeout: 공통 설정에서 각각 5,000ms, 인코딩 UTF-8

`SmtpVerificationEmailSender.sendVerificationCode(recipient, code)`는 전달받은 코드를 일반 텍스트 메일로
발송합니다. 발송 클래스는 코드를 생성·저장·검증하지 않으며 자동 재시도하지 않습니다. 발급 서비스가
코드 생성·Redis 보관을 담당하고, SMTP 실패는 `EMAIL_DELIVERY_FAILED`로 변환해 API에서 502로 응답합니다.
`app.mail.enabled`는 앱에서 추가한 선택 옵션이며 기본값은 `false`입니다. 환경변수는 `APP_MAIL_ENABLED`입니다.
`false`이면 `JavaMailSender`와 발송 컴포넌트를 등록하지 않습니다. SMTP 설정이나
`spring.mail.test-connection=true`가 남아 있어도 시작 시 SMTP에 연결하지 않습니다.
`true`이면 Boot의 메일 자동 설정을 활성화하며 SMTP 호스트와 발신 주소가 필요합니다.
`spring.mail.test-connection`은 연결 검사 여부만 제어하며 기능 전체를 끄는 옵션은 아닙니다.

```properties
# SMTP 없이 실행
app.mail.enabled=false
# SMTP 사용 시 위 값을 true로 변경
# app.mail.enabled=true
```

공유 예시는 비활성화 상태입니다. 실제 SMTP 설정을 준비한 뒤 개인 파일에서 활성화합니다.
SMTP를 활성화하면 `app.mail.from`을 반드시 지정해야 합니다. 공통 설정은 시작 시 연결 검사를 끄지만,
외부 설정 예시는 `spring.mail.test-connection=true`로 연결·인증을 검사합니다. 이때 SMTP 연결이나 인증에
실패하면 앱 시작도 실패할 수 있습니다. 메일을 발송하는 검사는 아닙니다.
외부 예시의 timeout 10,000ms는 공통 설정의 5,000ms보다 우선합니다.
후속 API를 연결할 때는 SMTP 설정 없이 발송 기능이 정상 동작한다고 처리하지 않아야 합니다.

### 실행 차단 조건

| 상황                                                    | 앱 시작 또는 요청에 미치는 영향                                                             |
|---------------------------------------------------------|---------------------------------------------------------------------------------------------|
| MySQL 연결 불가·미적용 스키마                           | JPA 초기화와 매핑 검증이 실패하면 앱 시작 중단. 먼저 DB 준비·마이그레이션 필요              |
| Redis 중지·앱 포트 불일치                               | 시작 시 검사는 없으며 메일 발급 요청 시 503. 앱 시작 성공은 Redis 준비 완료를 의미하지 않음 |
| `app.mail.enabled=false`                                | SMTP 설정·연결 검사 옵션이 남아 있어도 메일 빈과 SMTP 시작 검사 비활성화                    |
| 메일 활성화 + SMTP 호스트 설정 누락 또는 발신 주소 공백 | 필요한 메일 빈 또는 필수 설정이 없어 앱 시작 실패                                           |
| 메일 활성화 + `spring.mail.test-connection=true`        | DNS·연결·TLS·인증 오류가 있으면 앱 시작 중단                                                |
| 메일 활성화 + `spring.mail.test-connection=false`       | 시작 시 SMTP 연결 생략. SMTP 연결·인증 오류는 실제 발송 시 발생                             |

메일을 사용하면서 SMTP 장애 때문에 앱 시작이 막히지 않게 하려면 다음과 같이 설정합니다.
발신 주소 등 필수 설정은 여전히 필요합니다.

```properties
app.mail.enabled=true
spring.mail.test-connection=false
```

`smtp.example.com`, `replace-with-*`, `no-reply@example.com`은 접속 가능한 실제 설정이 아닙니다.
예시 파일을 복사한 뒤 메일 기능을 켜려면 공급자가 발급한 연결 정보와 허용된 발신 주소로 교체합니다.
SMTP 계정과 발신 주소는 서로 다른 설정이며, 로그인 계정이 발신 이메일 주소라는 보장은 없습니다.

운영에서도 비밀값을 소스에 넣지 않고 외부 설정으로 주입합니다. 대응 환경변수는 다음과 같습니다.

| 설정                   | 환경변수                                                                         |
|------------------------|----------------------------------------------------------------------------------|
| Redis 호스트·포트·DB   | `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`, `SPRING_DATA_REDIS_DATABASE` |
| Redis 인증             | `SPRING_DATA_REDIS_USERNAME`, `SPRING_DATA_REDIS_PASSWORD`                       |
| 메일 기능 사용 여부    | `APP_MAIL_ENABLED`                                                               |
| 시작 시 SMTP 연결 검사 | `SPRING_MAIL_TESTCONNECTION`                                                     |
| SMTP 서버·포트         | `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`                                           |
| SMTP 계정              | `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`                                   |
| 메일 발신 주소         | `APP_MAIL_FROM`                                                                  |

환경변수는 파일보다 우선하며 운영 SMTP의 TLS·인증 설정도 별도로 주입해야 합니다.
`SMTP_HOST`, `EMAIL_ENABLED` 등 다른 프로젝트의 키를 그대로 읽지는 않습니다.
Docker의 `REDIS_PORT`는 컨테이너 공개 포트이며 앱의 `SPRING_DATA_REDIS_PORT`와 별도로 맞춥니다.
SMTP 계정이 없는 환경의 **실제 메일 발송·수신은 미확인**입니다. 앱 시작이나 컴파일 성공은 발송 검증이 아닙니다.

인프라 구성에 이어 인증 코드 발급·재발송 요청 API와 CSRF 최초 쿠키 발급을 구현했습니다.
[발급 정책·API·오류·실패 처리 명세](docs/email-verification.md)를 참고하세요.
코드 검증·입력 실패 제한·가입용 증명 발급과 실제 회원가입까지 구현했습니다.
[회원가입 API·증명 소비·DB 실패 및 재시도 정책](docs/signup.md)과 [JWT 로그인](docs/login.md)을 참고하세요.
인증 테스트는 개발 DB·Redis와 분리된 임시 컨테이너에서 실행합니다. [인증 테스트 안내](docs/auth-testing.md)를 참고하세요.

후속 체크리스트:

- [x] #28: 가입 증명 일회성 소비·회원가입·동시 가입의 MySQL/Redis 통합 검증
- [x] #30: 실제 CSRF 쿠키·헤더를 사용한 필터 통합 검증
- [x] #30: Bearer 헤더 인증 경로와 Refresh 쿠키 경로별 CSRF 정책 구성
- [ ] #34: DB·Redis·선택적 SMTP 상태 점검, 생존·요청 준비 상태 및 접근 정책 구성
- [ ] #24: 실제 SMTP 수신·프론트 연동과 추가 만료·제한·장애 조합 검증 ([현재 범위](docs/auth-testing.md))

설정 근거: [Spring Boot Redis 연결](https://docs.spring.io/spring-boot/reference/data/nosql.html#data.nosql.redis.connecting),
[메일 자동 설정과 timeout](https://docs.spring.io/spring-boot/reference/io/email.html).

## 인증 접근 규칙 (기반 설정)

일반 API는 `Authorization: Bearer <Access Token>`으로 인증합니다.
로그인 응답은 Access Token을 본문에, Refresh Token을 HttpOnly 쿠키에 발급합니다.
갱신·로그아웃은 Refresh 쿠키를 사용하며, `/api/auth/**`의 변경 요청에는 CSRF 검증을 적용합니다.

| 요청                                                                                             | 접근 규칙                                                                      |
|--------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| `GET /api/auth/csrf`                                                                             | 인증 없이 CSRF 쿠키 준비                                                       |
| `POST /api/auth/email-verifications`                                                             | 인증 없이 메일 발급 요청 가능, CSRF 검증 적용                                  |
| `POST /api/auth/email-verifications/verify`                                                      | 인증 없이 코드 검증·가입용 증명 발급 가능, CSRF 검증 적용                      |
| `POST /api/auth/signup`                                                                          | 인증 없이 가입용 증명으로 회원가입, CSRF 검증 적용                             |
| `POST /api/auth/login`                                                                           | 이메일·비밀번호 확인 후 Access Token과 Refresh 쿠키 발급, CSRF 검증 적용       |
| `POST /api/auth/refresh`, `/api/auth/logout`                                                     | Refresh 쿠키 사용, CSRF 검증 적용. Access Token은 사용하지 않음                |
| 계정 본인 확인·비밀번호 변경·회원 탈퇴                                                           | Bearer 인증과 CSRF 검증 모두 필요                                              |
| `GET /api/users/me`                                                                              | Bearer 인증 필요                                                               |
| `GET /swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs`, `/v3/api-docs/**`, `/v3/api-docs.yaml` | 인증 없이 조회 가능, 문서 기능은 기본적으로 local에서만 활성화                 |
| `/api/admin`, `/api/admin/**`                                                                    | `ROLE_ADMIN` 권한 필요                                                         |
| 공개 화면·`/index.html`·`/assets/**`·`/brand/**`·`/fonts/**`·`/images/**`                        | GET·HEAD만 인증 없이 조회 가능 ([화면 경로](#프론트엔드를-포함한-배포용-빌드)) |
| 그 외 요청                                                                                       | 인증 필요                                                                      |

인증 상태는 HTTP 세션에 저장하거나 기존 세션에서 복원하지 않습니다.
기본 로그인 화면, HTTP Basic, 기본 로그아웃과 원래 요청의 세션 저장도 비활성화했습니다.
내부 ERROR 디스패치만 허용하며, 외부에서 직접 `/error`로 접근하는 요청은 공개하지 않습니다.
인증되지 않은 일반 보호 요청은 401, 인증된 회원의 권한 부족은 403으로 처리합니다.
MVC 예외와 Security 필터 예외는 `application/problem+json` 형식과 `code` 필드를 공유합니다.
`AuthErrorCode`는 공통 `ErrorCode`로 실패 종류·코드·공개 문구를 정의하고, `ApiProblems`가 실패 종류를 HTTP 상태로 변환합니다.
인증 MVC 예외도 `GlobalExceptionHandler`에서 처리하며, 기존 Refresh 쿠키 만료·캐시 금지·재시도 및 인증 헤더 조건을 유지합니다.
Security 필터의 JWT·권한·CSRF 오류는 기존 `SecurityProblemHandler`가 처리합니다.

`GlobalExceptionHandler`는 HTTP 응답을 만드는 웹 계층 구성요소이므로 Application의 `AuthException`을 참조할 수 있습니다.
현재 추가 응답 정책은 인증에만 있으므로, `handleAuth()`에서 쿠키·헤더 처리를 유지합니다. 글로벌 핸들러가 인증 예외와
Refresh 쿠키 컴포넌트를 아는 결합은 허용하고, 별도 응답 확장 인터페이스는 추가하지 않습니다.
다른 기능에도 전용 응답 정책이 늘어나면 공통 응답 확장 계약과 기능별 구현체로 분리하는 방식을 검토합니다.

| 상황                | HTTP 상태 | code                                                |
|---------------------|-----------|-----------------------------------------------------|
| 인증 필요           | 401       | `UNAUTHENTICATED`                                   |
| 권한 부족           | 403       | `ACCESS_DENIED`                                     |
| CSRF 토큰 누락·오류 | 403       | `INVALID_CSRF_TOKEN`                                |
| 입력값 검증 실패    | 400       | `INVALID_INPUT` (`errors`에 field/reason 포함)      |
| 예상하지 못한 오류  | 500       | `INTERNAL_SERVER_ERROR` (내부 정보는 로그에만 기록) |

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
일반 Bearer API는 CSRF 검사에서 제외하고, 로그인·갱신·로그아웃을 포함한 auth 변경 요청은 검사합니다. SPA 쿠키 → 헤더 CSRF 처리는 구성했으며, 실제 프론트엔드의 토큰 전달과 CORS 허용
출처는
연동 단계에서 구성합니다. 현재 브라우저 연동 안내는 동일 출처 기준입니다.

접근 정책 테스트는 테스트 전용 API와 인증 정보를 사용합니다. JWT 검증 성공을 의미하지 않습니다.

```bash
./backend/gradlew -p backend test --tests '*SecurityConfigTests'
```

## Swagger / OpenAPI

`local` 프로필 실행 후 `http://localhost:8080/swagger-ui/index.html`에서 확인합니다.
JSON 명세는 `/v3/api-docs`, YAML 명세는 `/v3/api-docs.yaml`입니다.
공통 설정은 문서 기능을 끄고, 클래스패스의 `application-local.properties`에서 활성화합니다.
외부 `config/application-local.properties`에는 개인 DB·Redis·SMTP 연결 정보를 둡니다.

현재 명세에는 가입·로그인·갱신·로그아웃·계정 본인 확인·비밀번호 변경·탈퇴와 내 정보 조회 API를 제공합니다.
로그인 응답의 Access Token을 Swagger Authorize의 `bearerAuth`에 입력하면 보호 API를 호출할 수 있습니다. 각 API 구현 시 실제 상태 코드에 맞춰
공통 응답을 참조하고, 보호 API에는 `@SecurityRequirement(name = "bearerAuth")`를 지정합니다.
auth 변경 API는 `csrfToken`을 보안 조건으로 표시합니다. Bearer/Refresh가 필요한 경우 같은 조건에 결합합니다.

`local` Swagger는 `springdoc.swagger-ui.csrf.enabled=true`로 CSRF 쿠키 값을 헤더에 자동 전달합니다.
페이지를 열면 쿠키가 준비되므로 CSRF를 복사하거나 Authorize의 `csrfToken`에 입력할 필요가 없습니다.
쿠키를 삭제했다면 페이지를 새로고침하거나 `GET /api/auth/csrf`를 실행합니다.
서버 검증은 동일하며 일반 클라이언트는 쿠키와 `X-XSRF-TOKEN` 헤더를 직접 전달해야 합니다.

실행 중인 앱에서 프론트엔드 전달용 파일을 생성합니다 (서버를 자동 기동하지 않습니다).

```bash
./backend/gradlew -p backend exportOpenApi
# 별도 포트로 실행했다면
./backend/gradlew -p backend exportOpenApi -PapiDocsUrl=http://127.0.0.1:18080/v3/api-docs
```

결과는 `backend/build/openapi/openapi.json`입니다. `info.version`은 Gradle 프로젝트 버전 (`0.0.1-SNAPSHOT`)입니다. Gradle 빌드 정보 없이 IDE에서
실행하면 버전은 `dev`로 표시됩니다.
API 변경 시 버전을 관리하고 전달 파일과 소스 커밋을 함께 기록합니다.
명세의 서버 URL은 `/`로 고정하여 개발자별 호스트·포트가 계약 파일에 섞이지 않게 합니다.
프론트엔드는 이 파일을 타입 생성기의 입력으로 사용할 수 있습니다.

프론트엔드의 `pnpm api:pull`도 이 내보내기 작업을 호출하며 명세 스냅샷과 생성 타입을 함께 갱신합니다.
이미 내보낸 파일은 `pnpm api:pull --file ../backend/build/openapi/openapi.json`으로 가져옵니다 (`frontend/`에서 실행). 재생성·변경
확인은 [프론트엔드 계약 안내](../frontend/README.md#openapi-계약-갱신)를 따릅니다.
응답 DTO의 필수 필드와 nullable을 명세에 표시하고, 업무·공통 오류 enum은 `ApiErrorCode`로 제공합니다.
`ApiProblem.code`는 Spring 기본 요청 오류나 새 코드도 받을 수 있도록 문자열 대체를 유지합니다.

테스트 구성과 실제 검증 범위는 [인증 테스트 안내](docs/auth-testing.md)를 참고하세요.

`localhost:8080` 내보내기가 404인데 브라우저에서는 정상이라면 IPv4/IPv6의 포트 점유가
다른지 확인하세요. 예를 들어 Spring 앱이 IPv6에서 실행 중이면 다음처럼 명시합니다.

```bash
./backend/gradlew -p backend exportOpenApi '-PapiDocsUrl=http://[::1]:8080/v3/api-docs'
```
