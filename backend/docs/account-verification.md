# 계정 관리용 이메일 본인 확인 (#30 4단계)

로그인한 회원의 현재 인증된 대표 이메일로 코드를 보내고, 비밀번호 변경 또는 회원 탈퇴에만
사용할 일회성 `accountProof`를 발급합니다. 이번 단계는 본인 확인과 증명 소비 기반까지입니다.
[비밀번호 변경·모든 기존 로그인 무효화](password-change.md)는 #30 5단계에서 연결했으며,
회원 탈퇴 실행 API는 다음 단계입니다.

## API와 브라우저 전달

두 POST API 모두 `Authorization: Bearer <accessToken>`과 `XSRF-TOKEN` 쿠키,
그 원문을 복사한 `X-XSRF-TOKEN` 헤더가 필요합니다. 먼저 `GET /api/auth/csrf`로
CSRF 쿠키를 준비합니다. Refresh 쿠키만으로는 인증되지 않습니다.
기존 `SecurityConfig`의 보호 경로와 `/api/auth/**` CSRF 범위가 그대로 적용됩니다.

### 코드 발급·재발송

`POST /api/auth/account-verifications`

```json
{"purpose":"PASSWORD_CHANGE"}
```

목적은 `PASSWORD_CHANGE` 또는 `ACCOUNT_DELETION`입니다. 회원 ID나 이메일은 입력받지 않고
Bearer 인증의 회원 ID로 DB에서 현재 이메일을 다시 조회합니다. 활성 상태이며 이메일 인증이
완료된 계정만 가능합니다. 이메일을 다른 값으로 지정해 인증할 수 없습니다.
메일 제목과 본문에도 요청한 작업을 표시합니다.

SMTP 서버에 발송 요청을 마치고 코드가 활성화되면 `202`, `Cache-Control: no-store`를 반환합니다.
수신함 도착을 보장하는 응답은 아닙니다. 실제 남은 시간에 따라 숫자는 달라집니다.

```json
{
  "verificationId":"7c5361d0-379b-4e54-8c29-fd44c4827ba7",
  "purpose":"PASSWORD_CHANGE",
  "expiresInSeconds":298,
  "resendAfterSeconds":58
}
```

### 코드 확인·증명 발급

`POST /api/auth/account-verifications/verify`

```json
{
  "purpose":"PASSWORD_CHANGE",
  "verificationId":"7c5361d0-379b-4e54-8c29-fd44c4827ba7",
  "code":"012345"
}
```

발급 때와 같은 회원·현재 이메일·목적·요청 ID에 대해 코드를 확인합니다.
성공하면 코드를 소비하고 `200`, `Cache-Control: no-store`로 다음 필드를 반환합니다.

| 필드 | 의미 |
| --- | --- |
| `accountProof` | 32바이트 난수를 Base64URL 인코딩한 43자 일회성 증명 |
| `purpose` | 확인한 작업 목적 |
| `expiresInSeconds` | 증명의 남은 유효기간. 기본 600초 |

증명은 JWT나 로그인 토큰이 아닙니다. 프론트엔드는 해당 작업을 진행하는 동안 메모리에 보관하고,
URL·로그·장기 브라우저 저장소에 넣지 않습니다. 이 문서는 전달 계약이며 프론트 구현은 포함하지 않습니다.
코드 확인 응답을 잃으면 원문 증명을 다시 조회할 수 없으므로 새 코드를 요청합니다.

## 실행 흐름과 책임

1. Security 필터가 Access Token과 현재 회원을 확인하고 CSRF를 검사합니다.
2. `AccountVerificationController`가 인증된 회원 ID와 검증된 요청을 서비스에 전달합니다.
   IP는 `getRemoteAddr()`를 사용하며 신뢰 프록시 설정 없이 `X-Forwarded-For`를 사용하지 않습니다.
3. `AccountVerificationService`가 `AccountLookup.findActiveVerifiedAccount`로 현재 이메일과 인증 버전을 조회합니다.
   발급·코드 확인·후속 증명 소비마다 다시 조회합니다.
4. 발급은 `AccountVerificationStore.reserve` → 목적별 SMTP 발송 → `activate` 순서입니다.
   SMTP 성공 전에는 코드가 `pending`이므로 확인할 수 없습니다.
5. 코드 확인은 Lua가 바인딩·시도 한도·다이제스트를 검사하고 코드 소비와 증명 생성을 원자 처리합니다.
6. 다음 단계의 변경 서비스가 `AccountVerificationService.consumeProof`를 호출하면 Lua가
   바인딩·유효기간·다이제스트·소비 상태를 확인한 뒤 한 번만 소비하고 확인 시각을 반환합니다.

Application은 조회·저장·메일 포트에만 의존합니다. JPA/Redis/SMTP는 Infrastructure,
HTTP·인증 principal·DTO·상태 코드는 Presentation에서 처리합니다. DB 스키마 변경은 없습니다.

## 저장·바인딩·제한

가입용 `sayroam:auth:{email-verification}:`와 별개로
`sayroam:auth:{account-verification}:` namespace를 사용합니다.
계정 관리 Lua의 키는 같은 Redis hash slot에 둡니다. 원문 이메일·IP·코드·증명은 저장하지 않고
이메일/IP/코드/증명에는 SHA-256 다이제스트를 사용합니다. 코드 다이제스트에는
회원 ID·이메일·목적·요청 ID도 포함합니다. 회원 ID는 Lua에서 문자열로 비교합니다.

| 상태/키 | 범위와 처리 |
| --- | --- |
| 코드·증명 | 회원 ID + 이메일 해시 + 목적 + 인증 버전. 값에도 동일 바인딩 저장·검사 |
| 재발송 대기 | 회원별, 두 목적 합산. 기본 60초 |
| 발송 횟수 | 회원별 시간창 5회, IP별 시간창 20회. 기본 시간창 3,600초 |
| 코드 | 6자리 숫자, 예약 시점부터 기본 300초. 실패 5회면 잠금 |
| 증명 | 코드 확인 성공 시부터 기본 600초. 소비해도 기존 TTL 유지 |

횟수 제한은 첫 승인부터 시작하는 고정 시간창입니다. 가입용 발송 횟수와는 별도입니다.
동일 목적의 재발급이 승인될 때 이전 코드와 증명을 삭제합니다. 제한으로 거절된 재발급은
기존 상태를 바꾸지 않습니다. 다른 목적의 코드·증명은 유지되지만 발송 제한을 우회할 수 없습니다.
이메일이 바뀌면 현재 이메일에 해당하는 키를 조회하므로 이전 이메일의 증명은 사용할 수 없습니다.
다른 회원·목적의 코드/증명과 가입용 `signupProof`는 사용할 수 없습니다.
비밀번호 변경으로 인증 버전이 증가하면 이전 코드·증명도 사용할 수 없습니다.

정책은 `app.auth.account-verification.*`로 설정하며 로컬 예시 파일에 전체 키를 제공합니다.
SMTP는 기존 `app.mail.enabled` 설정을 공유합니다. 비활성화면 Redis 예약 전에 실패하고,
기존 코드 확인·증명 소비에는 SMTP가 필요하지 않습니다.

## 오류·동시 요청·재시도

응답은 공통 `application/problem+json`, `code`, `Cache-Control: no-store` 형식입니다.

| HTTP | code | 처리 |
| --- | --- | --- |
| 400 | `INVALID_INPUT`, `EMAIL_CODE_INVALID` | 형식 오류 또는 코드 불일치. 유효한 현재 요청의 오답만 시도 횟수 증가 |
| 401 | `UNAUTHENTICATED`, `AUTHENTICATION_REVOKED` | Access Token 누락·만료·변조·탈퇴·인증 버전 불일치. CSRF가 먼저 실패하면 403 가능 |
| 403 | `INVALID_CSRF_TOKEN`, `ACCESS_DENIED`, `ACCOUNT_VERIFICATION_NOT_ALLOWED` | CSRF·권한 또는 인증된 대표 이메일 없는 계정 |
| 409 | `EMAIL_REQUEST_SUPERSEDED`, `EMAIL_CODE_ALREADY_USED` | 교체·대기·발송 중 만료된 요청 또는 확인 완료 코드 |
| 410 | `EMAIL_CODE_EXPIRED` | 코드가 없거나 만료. 다른 회원/이메일/목적의 키가 없을 때도 해당 |
| 429 | `EMAIL_RATE_LIMITED` | 발송 제한. `Retry-After`와 `retryAfterSeconds` 이후 새 발급 요청 |
| 429 | `EMAIL_CODE_ATTEMPTS_EXCEEDED` | 입력 한도 도달. 발송 제한에 따라 새 코드 요청 |
| 502 | `EMAIL_DELIVERY_FAILED` | SMTP 실패. 해당 pending 요청만 폐기하고 발송 제한은 유지 |
| 503 | `EMAIL_DELIVERY_DISABLED`, `EMAIL_VERIFICATION_UNAVAILABLE`, `AUTHENTICATION_UNAVAILABLE` | SMTP 비활성화·Redis 또는 회원 DB 장애. 성공으로 처리하지 않음 |

후속 증명 소비에는 `ACCOUNT_PROOF_INVALID`(400), `ACCOUNT_PROOF_EXPIRED`(410),
`ACCOUNT_PROOF_ALREADY_USED`(409)를 사용합니다. 다른 회원/이메일/목적에 증명이 아예 없으면
`EXPIRED`로 거절할 수 있습니다. 잘못된 증명으로 다른 정상 증명을 소비하지 않습니다.

- 같은 회원의 동시 발급은 공통 쿨다운/횟수 제한에서 직렬화합니다. 새 요청이 승인되면 이전 요청의
  늦은 활성화·실패 정리가 새 요청을 덮거나 삭제하지 않습니다.
- 같은 코드를 동시에 확인하면 한 요청만 증명을 발급합니다. 같은 증명도 한 번만 소비합니다.
  중복 호출에 같은 성공 응답을 재생하는 기능은 없습니다.
- SMTP 실패 시 pending 정리도 실패하면 저장소 오류를 우선 반환합니다. pending 상태로는 확인할 수 없고
  TTL에 따라 사라집니다. SMTP가 메일을 수락했지만 응답을 잃은 경우 메일이 도착할 수도 있습니다.
- 서비스에서 자동 재시도하지 않습니다. Redis 타임아웃·연결 단절·응답 유실은 이미 실행된 변경을
  되돌렸다는 뜻이 아닙니다. Lettuce 재연결/큐 재전송을 별도로 제한하지 않은 현재 구성에서는
  네트워크 장애 전체에 대한 정확히 한 번 실행을 보장하지 않습니다.
- 발급/확인 결과가 불확실하면 자동 재전송하지 말고 발송 제한 이후 새 코드를 요청합니다.
  Redis 재시작·데이터 소실 때도 임시 코드를 복구하거나 메모리 저장소로 우회하지 않습니다.
- 증명 소비는 후속 DB 변경과 분리되며 복원하지 않습니다. 다음 단계는 작업 목적을 서버 상수로 정하고,
  DB 트랜잭션에서도 현재 회원·이메일·상태·인증 버전을 확인해야 합니다.
  비밀번호 변경은 해시 변경과 버전 증가를 한 DB 트랜잭션으로 처리하며, 탈퇴 실행은 후속 단계입니다.

## 확인 범위

추가 테스트 작성·실행은 이슈 지침대로 인증 기능 전체 구현 후 진행합니다.
컴파일·패키징·앱 시작·OpenAPI/Swagger 노출 확인은 실제 메일 발송·코드 확인·증명 소비의
기능 검증과 구분합니다. 회원/목적/이메일 바인딩, 만료·제한·재사용·동시 요청, 실제 CSRF,
SMTP/Redis/DB 장애와 응답 유실 시나리오는 최종 검증 대상입니다.
