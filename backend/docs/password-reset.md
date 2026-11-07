# 비밀번호 찾기·재설정 (#75)

비밀번호를 잊은 사용자가 로그인 없이 이메일 소유를 확인하고 기존 비밀번호를 재설정합니다.
로그인 중 변경은 [비밀번호 변경](password-change.md), 가입용 인증은 [이메일 인증](email-verification.md)으로 분리합니다.
이번 범위는 백엔드와 연동 계약이며 화면은 #76에서 연결합니다.

## 요청 순서

세 요청 모두 Bearer 헤더 없이 호출하며 `GET /api/auth/csrf`로 받은 `XSRF-TOKEN` 쿠키와
`X-XSRF-TOKEN` 헤더가 필요합니다. 이메일은 앞뒤 공백 제거·소문자화합니다.

| 순서 | 요청 | 성공 |
| --- | --- | --- |
| 1 | `POST /api/auth/password-reset-verifications` — `{ "email": "user@example.com" }` | 202: `verificationId`, `expiresInSeconds`, `resendAfterSeconds` |
| 2 | `POST /api/auth/password-reset-verifications/verify` — `{ "email": "user@example.com", "verificationId": "<UUID>", "code": "012345" }` | 200: `resetProof`, `expiresInSeconds` |
| 3 | `POST /api/auth/password-reset` — `{ "email": "user@example.com", "resetProof": "<proof>", "newPassword": "<new password>" }` | 204, 현재 브라우저 Refresh 쿠키 만료 |

모든 성공·업무 오류는 `Cache-Control: no-store`입니다. 코드·증명·비밀번호를 로그나 브라우저 영구 저장소에 남기지 않습니다.
코드·증명은 가입·로그인 중 비밀번호 변경·탈퇴에 사용할 수 없습니다.

## 계정 여부를 드러내지 않는 발송

미가입·소셜 전용·탈퇴·미인증 이메일에도 같은 안내 메일과 코드를 보내고 같은 응답 필드·SMTP·실패 경로를 사용합니다.
기존 비밀번호 로그인 계정이 있으면 회원 ID·이메일·현재 인증 버전에 묶고, 없으면 비밀번호를 변경할 수 없는 대상으로 묶습니다.
따라서 202나 코드 확인 성공은 가입·재설정 가능 여부를 보장하지 않습니다. 화면에는
“코드를 보냈습니다. 기존 이메일·비밀번호 계정만 재설정할 수 있으며 소셜 가입자는 해당 소셜 로그인을 이용해 주세요.”처럼 안내합니다.
SMTP 장애도 계정 종류와 무관하게 같은 502를 반환합니다. SMTP 수락은 실제 수신함 도착을 보장하지 않습니다.

실제 저장은 활성·이메일 인증·기존 비밀번호 자격 정보가 있는 회원에게만 허용합니다.
소셜 전용 계정에 비밀번호를 생성하거나 기존 계정과 자동 연결하지 않습니다.
회원 ID·이메일·인증 버전이 발송 이후 바뀌면 코드·증명을 사용할 수 없으며 새 코드가 필요합니다.

## 유효기간과 제한

Redis는 `sayroam:auth:{password-reset}:` 전용 namespace를 사용합니다. 키의 이메일·IP와 코드·증명은 SHA-256으로 저장합니다.
기존 계정 관리의 Lua 스크립트를 공유해 예약·코드 확인·증명 소비를 원자 처리하며, 계정 관리 enum에 재설정 목적을 추가하지 않습니다.

`app.auth.password-reset.*` 기본 설정:

| 설정 | 기본값 |
| --- | --- |
| `code-ttl-seconds` | 300초, 6자리 숫자 |
| `proof-ttl-seconds` | 300초, 256비트 난수 증명 |
| `resend-cooldown-seconds` | 60초 |
| `rate-window-seconds` | 3600초 |
| `max-requests-per-email` / `max-requests-per-ip` | 5회 / 20회 |
| `max-verification-attempts` | 코드당 5회 |

발송 제한은 계정 유무·인증 버전과 무관하게 이메일·서버가 확인한 원격 IP 기준으로 유지합니다.
클라이언트의 `X-Forwarded-For`를 직접 신뢰하지 않습니다. 프록시를 사용하면 별도 신뢰 경계 설정이 필요합니다.
제한 초과 요청은 기존 코드·증명을 교체하지 않습니다. 재발급 승인 시 이전 코드·증명을 폐기합니다.
SMTP 실패 시 해당 대기 코드를 폐기하지만 발송 제한은 유지합니다. 메일 비활성화 시 예약 전에 503으로 실패합니다.

## 저장과 재시도

새 비밀번호에는 기존 8~128자 정책을 적용하고 공백이나 대소문자를 바꾸지 않습니다.
입력·정책 검증과 해싱을 증명 소비 전에 처리합니다. 증명은 일회성 소비하며 DB 실패 시 복원하지 않습니다.
기존 `PasswordChange` 포트가 회원 → 자격 정보 순서로 잠그고 회원 상태·이메일·인증 버전을 다시 확인한 뒤
비밀번호 해시·변경 시각·인증 버전 증가를 하나의 DB 트랜잭션에서 커밋합니다. 동시 재설정은 한 요청만 반영됩니다.

완료 후 모든 기기의 이전 Access/Refresh는 이후 인증·갱신에서 거절됩니다. 자동 로그인하지 않습니다.
화면은 메모리 토큰·회원 정보·진행 중 갱신을 정리하고 다른 탭에도 인증 무효화를 전달한 뒤 일반 로그인으로 안내합니다.
이미 열린 채팅 연결의 즉시 종료는 #79의 별도 범위입니다.

| 상태 / 코드 | 다음 행동 |
| --- | --- |
| 400 `INVALID_INPUT`, `PASSWORD_POLICY_VIOLATION` | 입력 수정. 서비스 진입 전/정책 오류는 증명을 소비하지 않음 |
| 400 `EMAIL_CODE_INVALID` | 남은 횟수 내 코드 재입력 |
| 400 `PASSWORD_RESET_PROOF_INVALID` | 잘못된 이메일·목적·증명·변경된 인증 버전·재설정 불가 계정. 원래 로그인 방식 또는 새 코드 사용 |
| 403 `INVALID_CSRF_TOKEN` | CSRF 준비. 변경 요청 자동 반복 금지 |
| 409 `EMAIL_REQUEST_SUPERSEDED`, `EMAIL_CODE_ALREADY_USED` | 최신 코드·이미 발급된 증명 사용 또는 새 코드 요청 |
| 409 `PASSWORD_RESET_PROOF_ALREADY_USED`, `PASSWORD_RESET_CONFLICT` | 재로그인으로 결과 확인 후 필요한 경우 새 코드 요청 |
| 410 `EMAIL_CODE_EXPIRED`, `PASSWORD_RESET_PROOF_EXPIRED` | 새 코드 요청 |
| 429 `EMAIL_RATE_LIMITED` | `Retry-After`와 `retryAfterSeconds` 이후 재요청 |
| 429 `EMAIL_CODE_ATTEMPTS_EXCEEDED` | 해당 코드 잠김. 재발송 제한 이후 새 코드 요청 |
| 502 `EMAIL_DELIVERY_FAILED` | 발송 실패·코드 무효화. 발송 제한 이후 새 요청 |
| 503 `AUTHENTICATION_UNAVAILABLE`, `EMAIL_VERIFICATION_UNAVAILABLE`, `EMAIL_DELIVERY_DISABLED` | 저장소·메일 상태 확인. 소비 결과 불확실 시 자동 반복하지 않음 |
| 503 `PASSWORD_RESET_UNAVAILABLE` | DB 결과 불확실. Refresh 쿠키 만료·클라이언트 인증 정리 후 새 비밀번호 로그인으로 결과 확인 |

204 응답을 잃었거나 증명 소비·DB 커밋 결과가 불확실하면 같은 재설정을 자동 재전송하지 않습니다.
비밀번호로 로그인해 결과를 확인하고 실패한 경우 새 이메일 인증부터 명시적으로 시작합니다.
성공 직후 같은 증명을 다시 보내면 변경된 인증 버전 때문에 400으로 거절될 수도 있습니다.

## 검증

`AuthIntegrationTests`는 실제 Security 필터·HTTP·테스트 전용 MySQL·Redis로 비로그인 재설정,
기존 로그인 무효화, 소셜 전용·미가입·탈퇴 차단, 목적/이메일 바인딩, CSRF, 만료·재발송·제한,
동시 요청, SMTP 포트 실패, DB 롤백과 증명 재사용·OpenAPI를 검증합니다.
실제 SMTP 수신과 #76의 브라우저 화면 연결은 이 자동 검증에 포함하지 않습니다.

보안 설계 참고: [OWASP Forgot Password Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html).
