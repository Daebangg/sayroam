# 이메일 인증 코드 발급·재발송 (#28)

이번 단계는 코드 발급과 발송 요청까지 구현합니다. 코드 검증, 입력 시도 제한, 가입 증명,
회원가입과 JWT는 아직 없습니다. 제공된 코드를 인증 완료나 로그인으로 처리하면 안 됩니다.

## 실행 조건

- 외부 `config/application-local.properties`는 `local` 프로필 + 실행 디렉터리 `backend/`에서 읽습니다.
- SMTP 사용에는 `app.mail.enabled=true`, 실제 SMTP 설정과 공급자가 허용하는 `app.mail.from`이 필요합니다.
- `spring.mail.test-connection=false`이면 SMTP 장애로 앱 시작이 중단되지는 않지만 실제 발급 요청은 실패할 수 있습니다.
- Redis 연결은 발급 요청 시 필요하며 서버 장애를 내부 메모리로 우회하지 않습니다.
- SMTP 비활성화 시 발급 API는 Redis를 사용하거나 발송을 시도하지 않고 503을 반환합니다.

## 정책

| 설정 | 기본값 | 의미 |
| --- | --- | --- |
| `app.auth.email-verification.code-ttl-seconds` | 300 | 발급 예약 시점부터의 코드 유효기간 |
| `app.auth.email-verification.resend-cooldown-seconds` | 60 | 같은 이메일의 최소 재요청 대기 시간 |
| `app.auth.email-verification.rate-window-seconds` | 3600 | 각 제한 키의 첫 요청부터 시작하는 집계 구간 |
| `app.auth.email-verification.max-requests-per-email` | 5 | 집계 구간의 이메일별 최대 발송 시도 수 |
| `app.auth.email-verification.max-requests-per-ip` | 20 | 집계 구간의 접속 IP별 최대 발송 시도 수 |

코드는 SecureRandom으로 만든 6자리 숫자이며 0으로 시작할 수 있습니다. 코드 원문을 API 응답이나 로그에
노출하지 않고 Redis에는 요청 ID와 결합한 SHA-256 다이제스트만 저장합니다. 짧은 숫자 코드의 다이제스트는
저장소가 유출됐을 때 추측 공격을 완전히 막지 못하므로 Redis 접근 통제는 여전히 필요합니다.
이메일·IP 원문은 Redis 키에 넣지 않고 각각 다이제스트를 사용합니다.

이메일은 앞뒤 공백 제거, ASCII 이메일 검증 후 전체 소문자로 정규화합니다. Gmail 점 제거·플러스 주소
합치기 등 공급자별 규칙은 적용하지 않습니다. 후속 회원가입·로그인에서도 같은 비교 기준을 사용해야 합니다.

제한 확인·횟수 증가·코드 교체는 Redis Lua에서 원자적으로 수행합니다. 같은 이메일의 새 발급을 예약하면
이전 요청은 즉시 무효화하며 새 요청은 `pending` 상태가 됩니다. SMTP 서버가 발송 요청을 수락한 뒤에만
같은 요청 ID를 `active`로 변경합니다. 코드를 검증할 때는 이메일·요청 ID·코드와 `active` 상태를 확인해야 합니다.
이 검증 API는 다음 단계에서 구현합니다. 성공 응답의 남은 유효기간에는 SMTP 처리 시간이 이미 차감됩니다.

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

두 시간은 응답 시점 기준 남은 시간을 초 단위로 올림한 값입니다. 현재 코드 확인 API가 없으므로
`verificationId`만으로 인증을 완료하거나 가입할 수 없습니다.

## 오류와 재시도

오류는 공통 `application/problem+json` 형식을 사용합니다. 실제 요청 경로는 `instance`로 제공됩니다.

| HTTP | code | 의미·다음 행동 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | 이메일 누락·형식·길이·ASCII 검증 실패. errors 확인 후 수정 |
| 403 | `INVALID_CSRF_TOKEN` | 쿠키·헤더 누락 또는 오류. CSRF 쿠키 준비 후 올바른 헤더 전달 |
| 409 | `EMAIL_REQUEST_SUPERSEDED` | SMTP 처리 중 요청이 만료·교체됨. 최신 요청 사용 |
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

## 확인 범위

추가 테스트는 작성·실행하지 않으며 전체 인증 구현 후 검증합니다. 이번 단계에서는 컴파일·패키징과
OpenAPI 생성 여부를 확인합니다. 코드 정상 발급·재발송, 제한·동시 요청·Redis 장애·SMTP 실패 정리,
실제 메일 발송·수신 및 브라우저 쿠키·헤더 검증은 아직 수행하지 않은 후속 검증 항목입니다.

관련 이슈: #28, #30, #34.
설정 근거: [Spring Security SPA CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).
