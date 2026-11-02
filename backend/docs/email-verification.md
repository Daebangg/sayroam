# 이메일 인증 코드 발급·검증과 가입용 증명 (#28)

코드 발급·재발송·검증·입력 실패 제한과 가입용 증명 발급을 구현합니다.
[회원가입 API](signup.md)에서 증명을 한 번 소비해 MySQL에 회원을 저장합니다. JWT·로그인은 아직 없습니다.
가입용 증명은 로그인 토큰이 아닙니다.

## 실행 조건

- 외부 `config/application-local.properties`는 `local` 프로필 + 실행 디렉터리 `backend/`에서 읽습니다.
- SMTP 사용에는 `app.mail.enabled=true`, 실제 SMTP 설정과 공급자가 허용하는 `app.mail.from`이 필요합니다.
- `spring.mail.test-connection=false`이면 SMTP 장애로 앱 시작이 중단되지는 않지만 실제 발급 요청은 실패할 수 있습니다.
- Redis 연결은 발급 요청 시 필요하며 서버 장애를 내부 메모리로 우회하지 않습니다.
- SMTP 비활성화 시 발급 API는 Redis를 사용하거나 발송을 시도하지 않고 503을 반환합니다.
- 이미 발급된 코드의 검증에는 SMTP가 필요하지 않으며 Redis가 필요합니다.

## 정책

| 설정 | 기본값 | 의미 |
| --- | --- | --- |
| `app.auth.email-verification.code-ttl-seconds` | 300 | 발급 예약 시점부터의 코드 유효기간 |
| `app.auth.email-verification.resend-cooldown-seconds` | 60 | 같은 이메일의 최소 재요청 대기 시간 |
| `app.auth.email-verification.rate-window-seconds` | 3600 | 각 제한 키의 첫 요청부터 시작하는 집계 구간 |
| `app.auth.email-verification.max-requests-per-email` | 5 | 집계 구간의 이메일별 최대 발송 시도 수 |
| `app.auth.email-verification.max-requests-per-ip` | 20 | 집계 구간의 접속 IP별 최대 발송 시도 수 |
| `app.auth.email-verification.max-verification-attempts` | 5 | 발급 요청별 코드 입력 실패 한도 (1~10) |
| `app.auth.email-verification.signup-proof-ttl-seconds` | 600 | 성공 시 생성하는 가입용 증명 TTL (60~3600초) |

코드는 SecureRandom으로 만든 6자리 숫자이며 0으로 시작할 수 있습니다. 코드 원문을 API 응답이나 로그에
노출하지 않고 Redis에는 요청 ID와 결합한 SHA-256 다이제스트만 저장합니다. 짧은 숫자 코드의 다이제스트는
저장소가 유출됐을 때 추측 공격을 완전히 막지 못하므로 Redis 접근 통제는 여전히 필요합니다.
이메일·IP 원문은 Redis 키에 넣지 않고 각각 다이제스트를 사용합니다.

이메일은 앞뒤 공백 제거, ASCII 이메일 검증 후 전체 소문자로 정규화합니다. Gmail 점 제거·플러스 주소
합치기 등 공급자별 규칙은 적용하지 않습니다. 회원가입도 같은 기준을 사용하며 후속 로그인에서도 이를 유지해야 합니다.

제한 확인·횟수 증가·코드 교체는 Redis Lua에서 원자적으로 수행합니다. 같은 이메일의 새 발급을 예약하면
이전 요청과 가입용 증명은 즉시 무효화하며 새 요청은 `pending` 상태가 됩니다. 제한에 걸려 거절된 발급 요청은 기존 상태를 바꾸지 않습니다. SMTP 서버가 발송 요청을 수락한 뒤에만
같은 요청 ID를 `active`로 변경합니다. 코드를 검증할 때는 이메일·요청 ID·코드와 `active` 상태를 확인해야 합니다.
발급 성공 응답의 남은 유효기간에는 SMTP 처리 시간이 이미 차감됩니다.

IP 제한은 서버가 확인한 `getRemoteAddr()` 기준입니다. 임의의 `X-Forwarded-For`를 직접 읽지 않습니다.
역방향 프록시 환경에서는 모든 요청이 프록시 IP로 집계될 수 있으므로 신뢰할 프록시와 전달 헤더 처리를
배포 시 별도로 정해야 합니다. 메일 주소와 IP를 바꾸는 분산 공격의 완전한 방어를 의미하지 않습니다.

## API

### GET /api/auth/csrf

인증 없이 호출할 수 있으며 204, `Cache-Control: no-store`와 `XSRF-TOKEN` 쿠키를 제공합니다.
이후 POST에는 같은 쿠키와 그 값을 복사한 `X-XSRF-TOKEN` 헤더를 함께 보냅니다.
Spring Security SPA 설정을 사용해 쿠키의 원문 헤더 토큰 검증과 BREACH 보호를 함께 처리합니다.
새 로그인·로그아웃 구현 시 쿠키가 초기화되므로 새 토큰 발급을 다시 연결해야 합니다.
동일 출처 브라우저 기준이며 CORS 출처 설정은 아직 포함하지 않습니다.

### POST /api/auth/email-verifications

인증 없이 호출할 수 있지만 CSRF 검증은 유지합니다. 최초 발급과 재발송은 같은 API입니다.

```http
POST /api/auth/email-verifications
Content-Type: application/json
Cookie: XSRF-TOKEN=<쿠키 값>
X-XSRF-TOKEN: <동일한 쿠키 값>

{"email":"user@example.com"}
```

성공하면 202와 `Cache-Control: no-store`를 반환합니다. SMTP 서버의 발송 요청 수락을 의미하며
실제 수신함 도착을 보장하지 않습니다. 인증 코드와 이메일을 응답에 넣지 않습니다.

```json
{
  "verificationId": "요청 ID",
  "expiresInSeconds": 297,
  "resendAfterSeconds": 57
}
```

두 시간은 응답 시점 기준 남은 시간을 초 단위로 올림한 값입니다. `verificationId`만으로 인증을 완료하거나 가입할 수 없습니다.

### POST /api/auth/email-verifications/verify

인증 없이 호출할 수 있지만 CSRF 쿠키·헤더는 발급 API와 동일하게 필요합니다.
이메일은 같은 정규화 규칙을 사용하고 verificationId는 UUID, code는 6자리 숫자 문자열입니다.

```json
{
  "email": "user@example.com",
  "verificationId": "00000000-0000-4000-8000-000000000000",
  "code": "012345"
}
```

성공하면 200, `Cache-Control: no-store`와 아래 응답을 반환합니다.

```json
{
  "signupProof": "가입용 난수 증명",
  "expiresInSeconds": 600
}
```

가입용 증명은 SecureRandom의 32바이트를 Base64URL로 인코딩한 43자 난수이며 JWT가 아닙니다.
원문은 성공 응답에만 넣고 Redis에는 SHA-256 다이제스트만 보관합니다. 요청·응답 DTO와 서비스 결과의
문자열 표현에서는 코드·증명을 가립니다. 로그나 URL에 원문을 넣지 않아야 합니다.

검증 Lua는 다음 순서로 처리합니다.

1. 상태의 TTL과 요청 ID를 확인합니다. 없는 요청과 만료는 같은 410 응답을 사용합니다.
2. `active`만 검증합니다. 교체·대기 중인 요청은 409이며 다른 요청의 실패 횟수를 늘리지 않습니다.
3. 올바른 요청의 코드 불일치에만 실패 횟수를 증가시킵니다. 입력 형식 오류는 실패 횟수에 포함하지 않습니다.
4. 기본 5번째 실패부터 `locked`로 바꾸고 코드 다이제스트를 제거합니다. 이후 정답을 넣어도 성공하지 않습니다.
5. 일치하면 가입용 증명을 저장하고 코드를 `verified`로 바꿔 다이제스트를 제거합니다.
6. 같은 코드의 후속 검증은 409로 실패하며 증명을 다시 발급하지 않습니다. 상태 TTL이 끝나면 410입니다.

기본 한도에서는 4회 틀린 뒤 5번째 입력이 맞으면 성공하고, 5번째도 틀리면 한도 초과입니다.
성공·시도 횟수 갱신·코드 소비·증명 저장을 같은 Lua에서 처리해 동시 검증의 중복 성공을 방지합니다.

증명은 동일한 이메일 해시의 `proof:` 키에 저장하며 `proofDigest`, `requestId`,
`verifiedAtEpochMillis` (Redis 서버 시각), `purpose=signup`, `status=available`과 TTL을 가집니다.
코드 상태는 기존 TTL을 유지하고 증명은 별도 TTL을 사용합니다. 코드가 만료돼도 이미 받은 증명은
자신의 TTL까지 남을 수 있습니다. 승인된 재발급은 이전 증명을 즉시 무효화하고 SMTP 실패 시에도 복구하지 않습니다.

회원가입에서는 입력 이메일로 같은 키를 선택하고 다이제스트·purpose·status·TTL을 검증한 후
증명을 원자적으로 소비합니다. 다른 이메일의 증명은 일치하지 않으며 이미 소비한 증명은 다시 사용할 수 없습니다.
DB 저장 전에 증명을 소비하고 저장 실패 시 복구하지 않습니다. [가입 실패·재시도 정책](signup.md)을 참고하세요.

검증 성공 직후 HTTP 응답을 잃거나 Redis 실행 결과가 불확실해지면 코드는 이미 소비됐을 수 있습니다.
증명 원문을 Redis에서 다시 꺼내거나 같은 코드로 재발급하지 않습니다. 증명을 받지 못했다면 발송 제한을
지켜 새 코드부터 다시 요청해야 합니다. 코드·증명 검증의 자동 재시도는 하지 않습니다.

## 오류와 재시도

오류는 공통 `application/problem+json` 형식을 사용합니다. 실제 요청 경로는 `instance`로 제공됩니다.

| HTTP | code | 의미·다음 행동 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | 이메일·요청 ID·코드의 필수값·형식 검증 실패. errors 확인 후 수정 |
| 400 | `BAD_REQUEST` | 잘못된 JSON 또는 UUID 역직렬화 실패. 입력 수정 |
| 400 | `EMAIL_CODE_INVALID` | 코드 불일치. 한도 안에서 다시 입력 |
| 403 | `INVALID_CSRF_TOKEN` | 쿠키·헤더 누락 또는 오류. CSRF 쿠키 준비 후 올바른 헤더 전달 |
| 409 | `EMAIL_REQUEST_SUPERSEDED` | 만료·교체·활성화되지 않은 요청. 최신 발급 요청 사용 |
| 409 | `EMAIL_CODE_ALREADY_USED` | 이미 검증한 코드. 받은 증명 사용, 증명을 못 받았다면 새 코드 필요 |
| 410 | `EMAIL_CODE_EXPIRED` | 코드가 없거나 만료됨. 새 코드 필요 |
| 429 | `EMAIL_CODE_ATTEMPTS_EXCEEDED` | 이 코드는 영구 잠금. 새 코드 필요, Retry-After 없음 |
| 429 | `EMAIL_RATE_LIMITED` | 응답의 Retry-After 헤더와 retryAfterSeconds 이후 다시 요청 |
| 502 | `EMAIL_DELIVERY_FAILED` | SMTP 실패. 새 pending 코드 삭제, 발송 제한 유지. 자동 재시도 없음 |
| 503 | `EMAIL_DELIVERY_DISABLED` | 메일 기능 비활성화. 운영 설정 확인 |
| 503 | `EMAIL_VERIFICATION_UNAVAILABLE` | Redis 장애 또는 응답 불확실. 저장소 복구 후 제한을 지켜 재요청 |
| 500 | `INTERNAL_SERVER_ERROR` | 예상하지 못한 오류 |

SMTP 실패 시 이전 코드를 복구하지 않으며 횟수·재발송 대기 제한도 되돌리지 않습니다.
같은 요청 ID의 `pending` 코드만 삭제하므로 늦은 실패 정리가 새로운 요청을 삭제하지 않습니다.
네트워크 오류는 SMTP 수락 이후에도 발생할 수 있어 502 응답만으로 메일이 절대 전달되지 않았다고
판단할 수 없습니다. 이 경우 도착한 코드는 유효한 발급 완료 상태로 처리하지 않습니다.

SMTP 수락 후 Redis 활성화가 실패하면 503을 반환하고 복구 후 재발급이 필요합니다. Redis 작업의
실행 결과가 네트워크 장애로 불확실할 때는 이미 코드가 바뀌었거나 제한이 소비됐을 수 있으므로
자동 재발송하지 않습니다. 이후의 정리·활성화는 요청 ID를 비교하며 만료된 코드를 되살리지 않습니다.

## 코드 구조와 오류 책임

```text
auth/
  application/
    service/EmailVerificationService
    port/out/EmailVerificationStore, VerificationEmailSender
    error/AuthErrorCode, AuthException
  presentation/
    controller/AuthCsrfController, EmailVerificationController
    dto/EmailRequest, EmailVerificationResponse, EmailVerificationConfirmRequest, EmailVerificationConfirmResponse
    error/AuthExceptionHandler
  infrastructure/
    config/EmailVerificationProperties
    redis/RedisEmailVerificationStore
    smtp/SmtpVerificationEmailSender
```

서비스는 저장·발송 포트에 의존합니다. SMTP 선택 의존성은 Java Optional로 전달하며 비활성화 시
Redis 예약 전에 실패합니다. 설정 바인딩은 infrastructure/config, 공통 연결 설정은 기존 global/config에 둡니다.

- `AuthErrorCode`: HTTP·Spring·공개 문구에 의존하지 않는 실패 식별자
- `AuthException`: 실패 코드, 선택적 Duration 대기시간, 원본 기술 예외 cause 보관
- Redis 어댑터: 발급·검증 결과와 제한·만료·소비 상태를 반환. 연결 등 DataAccessException은 애플리케이션 오류로 변환
- SMTP 어댑터: MailException을 발송 실패로 변환하고 cause 보존
- 서비스: 발급·검증 결과를 실패로 판단하고 SMTP 실패 시 같은 pending 요청 정리, 가입용 증명 생성
- `AuthExceptionHandler`: HTTP 상태·공개 문구·ProblemDetail·Retry-After를 한곳에서 결정

HTTP 503은 현재 인증 기능을 처리할 수 없음을 표현하며 메일 비활성화·Redis 장애에 사용합니다.
SMTP 발송 실패의 기존 502 응답은 유지합니다. 503 자체가 앱 프로세스 종료를 의미하지는 않습니다.
시작 시 SMTP 연결 검사에 따른 종료 조건과 API 요청 처리 중의 HTTP 실패 응답은 별개입니다.

cause·suppressed는 내부 진단용이며 응답에 포함하지 않습니다. 어댑터 로그는 예외 타입만 기록하고
원문 SMTP 응답·메일 내용·접속 비밀값을 그대로 출력하지 않습니다. Redis 정리 장애가 나면 그 장애를
우선 전달하되 SMTP 실패도 suppressed로 보존합니다. 예상하지 못한 프로그래밍 오류는 AuthException으로
일괄 변환하지 않고 기존 글로벌 500 처리로 보냅니다. 누락·비정상 형식의 Lua 결과도 예상 밖 오류로 처리합니다.

기존 공통 Security/MVC 오류 처리를 재사용하며 MySQL 회원 저장은 회원가입 어댑터에서 담당합니다.
별도 ErrorResponse를 추가하지 않고 ProblemDetail 계약과 기존 OpenAPI 스키마 이름을 유지합니다.

## 확인 범위

추가 테스트는 작성·실행하지 않으며 전체 인증 구현 후 검증합니다. 이번 단계에서는 컴파일·패키징과
OpenAPI 생성 여부를 확인합니다. 코드 정상 발급·검증·재발송, 입력 제한·중복 성공 방지, 증명 저장·만료·재발급 무효화,
동시 요청·Redis 장애·SMTP 실패 정리,
실제 메일 발송·수신 및 브라우저 쿠키·헤더 검증은 아직 수행하지 않은 후속 검증 항목입니다.

관련 이슈: #28, #30, #34.
설정 근거: [Spring Security SPA CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

코드·토큰 취급 기준 참고: [OWASP OTP 처리](https://cheatsheetseries.owasp.org/cheatsheets/Multifactor_Authentication_Cheat_Sheet.html#one-time-password-otp-handling-and-storage), [이메일 검증](https://cheatsheetseries.owasp.org/cheatsheets/Email_Validation_and_Verification_Cheat_Sheet.html).
