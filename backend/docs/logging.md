# 로그 구성과 요청 추적 (#219)

애플리케이션은 기존 SLF4J·Logback과 Spring Boot 내장 JSON 출력을 사용합니다.
별도 로그 수집 서버나 로깅 의존성을 추가하지 않습니다.

## 환경별 출력

| 환경 | 출력 | 기본 수준 |
| --- | --- | --- |
| `local` 프로필 | 요청 결과를 간결하게 표시하는 색상 콘솔 텍스트 | 앱 INFO, 그 외 WARN |
| 그 외 프로필 | 표준 출력의 Logstash JSON | INFO |

`make backend-run`은 `local` 프로필을 선택합니다. 운영에서는 `local`을 활성화하지 않고 기존 환경변수로
앱을 실행합니다. JSON은 요청·작업 MDC와 SLF4J의 `addKeyValue` 필드를 포함합니다.
설정은 `src/main/resources/logback-spring.xml`과 `application*.properties`에서 관리합니다.

로컬 요청 로그는 다음처럼 표시합니다. 메서드는 청록, 경로는 파랑, 응답 코드는 2xx·3xx 초록 /
4xx 노랑 / 5xx 빨강, 처리 시간은 자홍으로 구분합니다. 시간은 흐리게 표시합니다.

```text
20:45:45.185 INFO POST /api/auth/refresh → 200 · 247 ms
20:45:45.229 INFO GET /api/users/me → 200 · 41 ms
20:45:45.284 INFO GET /api/destinations → 200 · 39 ms
```

요청 필드는 고정 너비 없이 한 칸으로 구분하며, 경로를 자르지 않습니다.
스레드·로거 이름·이벤트 키·추적 ID는 정상 요청 줄에서 숨깁니다. 요청 ID·작업 ID 등 원본 MDC와 검색 필드는
유지되며 운영 JSON에 포함됩니다. 예상하지 못한 서버 장애는 다음 상세 줄에 요청 ID와 안전한 호출 위치를 표시합니다.
업무 오류는 결과 줄 뒤에 오류 코드만 추가합니다. 로컬 요청 표시 형식은 `logging.pattern.request-console`로 조정할 수 있습니다.
필요할 때 패턴에 `%X{requestId}`·`%X{taskId}`·`%X{jobId}`를 추가해 전체 추적 ID를 표시할 수 있습니다.

일반 앱 로그는 시간·수준·로거·메시지·검색 필드를 표시합니다. Gradle·IDE 콘솔의 자동 감지 차이를
피하기 위해 `local`에서 ANSI 출력을 켭니다. 색이 필요 없으면 `SPRING_OUTPUT_ANSI_ENABLED=never`로 끌 수 있습니다.

```bash
# 특정 기능만 일시적으로 상세 로그 확인
LOGGING_LEVEL_COM_DAEBANGG_SAYROAM_COMPANION=DEBUG make backend-run
# HTTP 완료 로그만 일시적으로 DEBUG까지 확인
make backend-run APP_ARGS='--spring.profiles.active=local --logging.level.com.daebangg.sayroam.global.web.RequestLoggingFilter=DEBUG'
# 반복 조회·정적 자원도 500ms 이상 걸리면 INFO
APP_LOGGING_HTTP_SLOW_REQUEST_MS=500 make backend-run
```

로컬 기본 화면에는 앱 기동 안내·일반 API 요청 완료·실패·경고를 남기고,
리포지토리 스캔·Hibernate/Hikari/Tomcat 초기화 등 프레임워크 INFO와 정상 반복 조회의 DEBUG는 숨깁니다.
상세 진단이 필요할 때 해당 기능의 로거만 잠시 DEBUG로 올립니다.

설정 우선순위는 기존 Spring 설정을 따릅니다. 운영에서 root·Spring Web·Security·HTTP 클라이언트·Hibernate
전체를 DEBUG/TRACE로 올리지 않습니다. 라이브러리 상세 로그는 URI·외부 응답·SQL 값 등 민감정보를 포함할 수 있습니다.
Hibernate의 JDBC 오류·경고 원문 및 SQL·바인딩·추출 로그는 기본 OFF입니다. DB 오류 메시지에 중복 이메일 등
실제 값이 들어갈 수 있어 앱에서 기록하는 오류 코드와 안전한 진단 필드로 확인합니다.

## 로컬 시작 요약과 실제 연결 검사

`local`에서는 `ApplicationReadyEvent`에서 실제 의존성 검사를 실행한 뒤 SayRoam 블록 문자 로고와 결과 패널을 표시합니다.
설정이 있다는 이유만으로 성공을 표시하지 않습니다. 각 항목에 수행한 검사와 소요 시간을 함께 표시합니다.

| 항목 | 실제 검사 | 성공 표시 |
| --- | --- | --- |
| MySQL | 앱의 DataSource로 연결을 얻어 `SELECT 1` 실행 후 반환값 확인 | `Verified` |
| Redis | 앱의 연결 설정으로 `PING`을 실행해 `PONG` 확인 | `Verified` |
| SMTP | 기존 Boot 시작 연결 검사가 이미 성공했다면 그 결과 사용. 그 외에는 `testConnection()` 실행 | `Verified` |
| Google·Kakao OAuth | 공개 OIDC 메타데이터를 실제 조회하고 앱에 등록한 인증·토큰 엔드포인트와 일치하는지 확인 | `Reachable` |
| Naver OAuth | 공식 인증 엔드포인트의 실제 2xx·3xx HTTP 응답 확인 | `Reachable` |

OAuth의 `Reachable`은 인증 서버 접속 확인입니다. 클라이언트 비밀값 유효성이나 사용자 로그인·토큰 교환 성공을
의미하지 않습니다. 이 검사에는 사용자 자격 정보나 클라이언트 비밀값을 전송하지 않습니다.
SMTP 검사도 연결·인증만 수행하며 테스트 메일을 발송하지 않습니다.
DB·Redis 검사는 읽기 전용이며 업무 데이터나 인증 세션을 수정하지 않습니다.

비활성 기능은 `Disabled`, 필요한 빈이 없으면 `Missing`, 실제 요청 실패는 `Failed`, 제한 시간 초과는 `Timeout`으로 표시합니다.
실패를 성공으로 바꾸거나 조용히 숨기지 않으며, 실패 원문 대신 안전한 예외 유형·호출 위치만 로그에 남깁니다.
연결 검사 실패만으로 개발 앱을 강제 종료하지는 않습니다.

검사는 병렬로 실행하고 전체 검사 대기 시간을 `app.logging.startup.timeout-ms`(기본 3000ms, 최대 30000ms)로 제한합니다.
실패·시간 초과 항목은 패널에 남깁니다. 운영 프로필에서는 이 개발용 검사·패널 빈을 등록하지 않습니다.
운영의 지속적인 상태 검사는 별도 Actuator/관측성 구성에서 수행합니다.

패널과 검사를 끄려면 `APP_LOGGING_STARTUP_ENABLED=false`로 실행합니다. 기존 기동 INFO를 다시 보려면
`SPRING_MAIN_LOG_STARTUP_INFO=true`도 함께 지정합니다. 운영 JSON 출력은 유지합니다.

## WebSocket / STOMP 로그

인증이 완료되어 브로커가 `CONNECTED`를 전송하면 INFO, 연결이 종료되면 연결 유지 시간과 WebSocket 종료 코드를
기록합니다. 정상 종료(1000)·이탈(1001)은 INFO, 그 외 종료는 WARN입니다. 같은 세션의 종료 이벤트는 한 번만 기록합니다.

STOMP 프레임 수신 단계에서 인증·권한·업무 오류로 거절하면 WARN, 예상하지 못한 서버·저장소 장애는 ERROR와 안전한 예외 진단을 남깁니다.
토큰 만료·권한 변경 등으로 서버가 연결을 닫도록 요청하면 `CLOSE`와 오류 코드를 기록하고,
실제 연결 해제는 별도의 `DISCONNECTED`로 표시합니다. 전송 채널이 종료 요청을 받지 않으면 `CLOSE_FAILED`로 표시합니다.

```text
21:30:00.100 INFO WS CONNECTED · session=ab12cd34
21:30:03.200 WARN WS CONNECT · UNAUTHENTICATED · session=ef56ab78
21:31:00.100 INFO WS CLOSE · ACCESS_DENIED · session=ab12cd34
21:31:00.105 INFO WS DISCONNECTED · 1000 · 60005 ms · session=ab12cd34
```

로컬은 HTTP와 같은 색상으로 고정 너비 없이 표시하고, 세션 ID는 앞 8글자만 보여 줍니다.
원본 이벤트·운영 JSON에는 전체 `sessionId`, `event`, `action` 및 해당 이벤트의 `closeCode`, `durationMs`, `errorCode`가 들어갑니다.
종료 사유 문자열·사용자 ID·토큰·헤더·채팅 본문은 기록하지 않습니다.

정상 `SUBSCRIBE`·`UNSUBSCRIBE`·`SEND` 프레임은 DEBUG이며 기본 화면에서 숨깁니다.
이 로그는 인증·권한 검사 후 수신 채널에 프레임이 전달됐다는 뜻이며, 메시지 저장·상대방 수신 완료를 의미하지 않습니다.
목적지는 허용된 경로 템플릿만 기록하고, 알 수 없는 목적지는 `UNMAPPED`로 표시합니다.

```bash
make backend-run APP_ARGS='--spring.profiles.active=local --logging.level.com.daebangg.sayroam.global.logging.WebSocketLog=DEBUG'
```

## HTTP 요청 기록

서버가 매 요청마다 UUID를 생성하고 `X-Request-ID` 응답 헤더에 제공합니다. 클라이언트가 보낸 같은 이름의
헤더는 재사용하지 않습니다. 문제 신고에는 발생 시각과 이 응답 ID를 함께 기록하면 됩니다.
응답 본문과 기존 인증·오류 코드·쿠키·상태 코드는 유지합니다.

요청 필터는 Security보다 먼저 실행하므로 컨트롤러 진입 전 거절된 401·403도 기록합니다.
완료 이벤트의 필드는 다음과 같습니다.

| 필드 | 내용 |
| --- | --- |
| `event` | `http_request_completed` |
| `requestId` | 응답 헤더와 같은 서버 생성 ID |
| `method` | 표준 HTTP 메서드, 비표준 메서드는 `OTHER` |
| `route` | Spring이 선택한 경로 템플릿. 진입 전 거절·없는 경로는 `UNMAPPED` |
| `status` | 완료 시 HTTP 상태 |
| `durationMs` | 단조 시계로 측정한 전체 처리 시간 |
| `errorCode` | MVC·Security가 처리한 오류의 공개 코드가 있는 경우 |
| `exceptionType`, `exceptionFrames` | 예기치 않은 장애에 한해 허용된 예외 유형·호출 위치 |

실제 경로 변수·검색어·쿼리 문자열·본문·헤더·쿠키·회원 이메일·IP는 완료 로그에 넣지 않습니다.
정상 API와 4xx는 INFO, 5xx는 ERROR입니다. 4xx에 스택을 반복 기록하지 않습니다.
예상하지 못한 MVC 예외는 예외 핸들러에서 요청에 표시하고, 완료 필터 한 곳에서 진단 정보를 기록합니다.

정상 정적 자원·화면·문서·OPTIONS 및 아래 GET 반복 조회는 DEBUG로 낮춥니다.
오류가 발생하거나 `app.logging.http.slow-request-ms`(기본 1000ms) 이상 걸리면 이 제한을 해제합니다.

- `/api/notifications`
- `/api/notifications/summary`
- `/api/rooms/{roomId}/messages/summary`

비동기 요청은 최초 필터 반환 시 완료로 기록하지 않고, Servlet 비동기 완료 시 최종 상태로 한 번 기록합니다.
재디스패치에도 같은 ID를 사용하고, 요청 스레드·완료 콜백의 MDC는 각각 이전 상태로 복구합니다.
새 HTTP 경로를 로그 표시용으로 별도 복제하지 않습니다. 경로 템플릿은 MVC의 매핑 결과만 사용합니다.

## 비동기·백그라운드 작업

`LoggingTaskDecorator`는 요청 ID만 전달하며 새 `taskId`를 부여합니다. 인증 정보나 MDC 전체를 복사하지 않습니다.
Boot 기본 실행기(MVC Callable 포함)와 매칭 실행기·복구 스케줄러에 적용하며, 실행 종료·실패 시 워커의 원래 MDC를 복구합니다.
메시지 브로커가 제공하는 Executor 빈 때문에 MVC가 별도 기본 실행기를 쓰지 않도록
`spring.task.execution.mode=force`로 Boot의 `applicationTaskExecutor` 생성을 보장합니다.
채팅 세션 만료 작업에도 적용하여 실행 실패를 안전한 진단 형식으로 기록합니다.

매칭 작업은 여러 요청의 작업을 한 실행에서 처리합니다. 따라서 최초 실행 신호의 요청 ID를 모든 작업에 붙이지 않고,
개별 작업마다 `jobId`와 새 `taskId`를 사용합니다. 재시도는 같은 `jobId`로 추적합니다. claim token은 기록하지 않습니다.
작업이 없는 정기 확인은 성공 로그를 남기지 않으며, 작업 완료는 DEBUG·처리 실패는 WARN입니다.
메일·Redis·외부 장소 검색 실패도 일정한 `event` 이름과 안전한 진단 필드를 사용합니다.
회원·방 관련 이벤트는 필요한 내부 숫자 ID만 남기며 이름·이메일·채팅 내용·세션 토큰을 기록하지 않습니다.

## 로그 작성 기준

- INFO: 요청 완료 등 운영 흐름을 확인할 수 있는 결과
- WARN: 외부 연동 실패·재시도할 작업·커밋 이후 알림 실패 등 복구 가능한 실패
- ERROR: 예상하지 못한 서버 장애·실행할 수 없는 작업
- DEBUG: 정상 반복 조회, 정적 자원, 작업 완료 등 개발 중 필요한 상세 정보

이벤트 이름·메시지는 코드에 정한 문자열을 사용하고, 필요한 검색 항목은 `addKeyValue`로 나눕니다.
오류는 `SafeLog.atWarn`·`SafeLog.atError`를 사용합니다. 이 헬퍼는 최상위 예외 및 최대 4단계 원인의 유형과
각 최대 8개 호출 위치를 남기며, 예외 메시지·suppressed·Throwable 원문은 출력하지 않습니다.
민감값이 오류 메시지에 섞여 있어도 로그로 전달되지 않습니다. 원인 순환과 과도한 스택 크기도 제한합니다.

```java
SafeLog.atWarn(log, exception)
        .addKeyValue("event", "matching_job_failed")
        .log("Companion matching job failed");
```

비밀번호·Access/Refresh Token·쿠키·인증 코드·OAuth state/증명·외부 API 키·메일 주소·개인 채팅 내용을
메시지나 검색 필드에 넣지 않습니다. DTO 전체·요청/응답 본문·원문 URI·`exception.getMessage()`·
`exception.toString()`·Throwable을 그대로 로깅하지 않습니다. JSON 출력 자체가 민감값을 지워주는 것은 아닙니다.
기존 예외 객체와 공개 응답은 수정하지 않으며, 진단 로그에 전달하는 정보만 제한합니다.

## 수집·보관

기본 출력 위치는 표준 출력 한 곳입니다. 애플리케이션 파일 appender와 이중 저장은 제공하지 않습니다.
현재 구성에서 `logging.file.name`만 지정해도 파일 저장이 활성화되는 것은 아닙니다.
운영 담당자는 배포 방식에 맞는 프로세스 관리자·컨테이너 런타임·외부 수집기로 표준 출력을 수집하고,
보관 기간·전체 용량 상한·접근 권한·자동 삭제를 배포 환경에 설정합니다. 이 저장소가 수집기를 설치하거나
보관을 보장하지는 않습니다. 로컬 터미널 출력도 필요 이상으로 장기 보관하거나 공유하지 않습니다.
파일 저장을 추가할 경우 일별·용량별 회전, 보관 기간·용량 상한·자동 정리를 함께 구성하고 검증해야 합니다.

## 검증

```bash
./backend/gradlew -p backend test --tests '*RequestLoggingFilterTest' --tests '*SafeLogTest' --tests '*LoggingTaskDecoratorTest'
./backend/gradlew -p backend build
```

회귀 검증은 요청 ID와 응답 연결, 원문 경로·헤더·본문 제외, 4xx/5xx 분류, 중복 오류 방지,
비동기 최종 상태, 요청·워커 MDC 정리, JSON 진단의 민감정보 제외를 확인합니다.
기존 인증·오류 응답 및 매칭·채팅 작업의 성공·실패 동작도 함께 검증합니다.
