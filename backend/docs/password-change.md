# 비밀번호 변경·모든 기존 로그인 무효화 (#30 5단계)

로그인한 회원이 `PASSWORD_CHANGE` 이메일 본인 확인 후 자신의 비밀번호를 변경합니다.
변경 완료 후 모든 기기의 기존 Access Token과 Refresh Token은 이후 인증·갱신에 사용할 수 없습니다.
회원 탈퇴는 별도 목적의 증명을 사용하는 [회원 탈퇴·계정 정리](account-deletion.md)를 따릅니다.

## API와 브라우저 흐름

1. `GET /api/auth/csrf`로 CSRF 쿠키를 준비합니다.
2. Bearer 인증과 CSRF 쿠키·헤더를 사용해
   `POST /api/auth/account-verifications`에 `{"purpose":"PASSWORD_CHANGE"}`를 보냅니다.
3. 같은 목적과 요청 ID로 `/api/auth/account-verifications/verify`에서 코드를 확인하고 `accountProof`를 받습니다.
4. 다음 요청으로 비밀번호를 변경합니다. 회원 ID·이메일·작업 목적은 요청에서 받지 않습니다.

```http
PATCH /api/auth/password
Authorization: Bearer <accessToken>
Content-Type: application/json
X-XSRF-TOKEN: <XSRF-TOKEN 쿠키 원문>
Cookie: XSRF-TOKEN=<같은 원문>
```

```json
{
  "accountProof":"<PASSWORD_CHANGE 확인에서 받은 43자 증명>",
  "newPassword":"<새 비밀번호>"
}
```

새 비밀번호는 가입과 같은 **8~24자·대문자·숫자·특수문자 각 1개 이상·공백/제어/숨김 문자 금지**입니다.
공백 제거·대소문자 변환을 하지 않습니다. 기존 8~128자 비밀번호로 로그인한 뒤 본인 확인을 거쳐
새 조건으로 변경할 수 있습니다. [공통 입력 정책](auth-input-policy.md)을 참고합니다.
도메인 값 객체 `Password`가 새 비밀번호를 검증하고, DTO의 길이 제약은 같은 상수를 사용합니다.
`PasswordChangeService`도 첫 단계에서 정책을 검사하므로 HTTP를 거치지 않는 호출도 증명 소비 전에 거절합니다.
현재 비밀번호 대신 이메일 일회성 증명으로 본인 확인합니다. 같은 비밀번호로 바꾸는 요청도
성공하면 인증 버전이 올라가 기존 로그인을 무효화합니다.
증명과 비밀번호는 응답·로그·URL에 넣지 않으며 요청 객체의 문자열 표현에서도 가립니다.

성공 응답은 `204 No Content`, `Cache-Control: no-store`와 현재 Refresh 쿠키 만료입니다.
발급과 같은 `Path=/api/auth`, HttpOnly·SameSite=Strict·Secure 설정을 사용합니다.
XSRF-TOKEN 쿠키는 유지하며 새 Access/Refresh Token은 발급하지 않습니다.
다른 기기의 쿠키를 서버가 직접 지울 수 없으므로 해당 기기의 다음 인증·갱신에서 거절합니다.

프론트는 성공 후 Access Token·회원 정보·남은 accountProof를 메모리에서 지우고 자동 갱신을 중단합니다.
같은 쿠키를 공유하는 탭에도 로그인 종료를 알리고, 변경 전에 시작한 로그인·갱신 응답이 늦게 도착해도
현재 로그인 상태를 되살리지 않아야 합니다. 로그인/갱신/비밀번호 변경을 직렬화하고 새 비밀번호로 재로그인을 안내합니다.
이 문서는 프론트 전달 계약이며 프론트 구현을 포함하지 않습니다.

## 실행 순서와 책임

1. `SecurityConfig`가 CSRF와 Bearer 인증을 검사합니다. `ActiveAccountJwtConverter`가 토큰의
   인증 버전과 DB의 현재 회원 버전을 비교하고 현재 회원 정보를 principal로 연결합니다.
2. `PasswordChangeController`가 principal의 회원 ID·인증 버전과 검증된 본문을 서비스에 전달합니다.
3. `PasswordChangeService`가 먼저 비밀번호 정책을 확인한 뒤 현재 인증된 이메일·인증 버전·기존 비밀번호 로그인 수단을 확인합니다.
   비밀번호가 없는 소셜 전용 계정은 증명 소비 전에 거절합니다. 계정 연결이나 비밀번호 신규 등록을 하지 않습니다.
4. 기존 `PasswordHasher`로 PBKDF2 해시를 만든 후 `AccountVerificationService.consumeProof`를 호출합니다.
   목적은 서버의 `PASSWORD_CHANGE` 상수이며, 회원·현재 이메일·목적·인증 버전·만료·다이제스트를 확인해 한 번만 소비합니다.
5. `AccountWriteService`가 새 트랜잭션에서 회원 → 자격 정보 순서로 쓰기 잠금을 획득합니다.
   현재 이메일·활성/이메일 인증 상태·예상 인증 버전·기존 자격 정보를 다시 확인합니다.
6. 비밀번호 해시와 UTC 변경 시각을 저장하고 `users.auth_version`을 1 증가시켜 **같은 MySQL 트랜잭션으로 커밋**합니다.
7. 커밋 성공 후에만 컨트롤러가 204와 쿠키 만료를 반환합니다. 커밋 뒤 Redis 정리 호출은 없습니다.

Application은 포트와 도메인 값만 사용합니다. JPA·잠금·트랜잭션은 Infrastructure,
HTTP·쿠키·입력 검증과 오류 명세는 Presentation에 둡니다.

## 모든 기존 로그인 차단

Flyway `V2__add_user_auth_version.sql`이 `users.auth_version BIGINT NOT NULL DEFAULT 0`을 추가합니다.
Access Token은 `auth_ver` 문자열 클레임, Redis Refresh 상태는 `authVersion` 문자열로 발급 시점 버전을 보관합니다.
회원 ID와 버전을 Lua 숫자로 변환하지 않아 BIGINT 정밀도를 잃지 않습니다.

- **Access Token:** 서명·만료 등 검증 후 매 요청 DB의 활성 회원과 인증 버전을 확인합니다.
  버전이 다르면 만료가 남아 있어도 401입니다. 인증 버전에는 시간 오차 허용을 적용하지 않습니다.
- **Refresh Token:** Redis inspect로 받은 버전과 DB 현재 버전이 같을 때만 같은 버전으로 Access를 서명하고 Refresh를 교체합니다.
  버전이 다르면 401이며 쿠키를 만료시킵니다. 기존 Redis 키는 절대 TTL까지 남을 수 있지만 갱신 권한은 없습니다.
- **로그인:** 비밀번호를 검증한 자격 정보 스냅샷의 버전으로 두 토큰을 발급합니다.
  이전 비밀번호 검증이 변경 전에 시작해 늦게 끝나도 토큰은 이전 버전이므로 변경 후 인증에 사용할 수 없습니다.
- **본인 확인:** 코드·증명도 인증 버전에 묶습니다. 이전 버전의 코드·증명은 새 로그인에서 사용할 수 없습니다.
  필터 통과 뒤 버전이 바뀐 진행 중 요청도 서비스/DB에서 예상 버전을 다시 확인합니다.

비밀번호 변경과 로그인 무효화 사이에 DB·Redis 분산 커밋은 필요하지 않습니다.
해시만 바뀌고 기존 로그인 차단만 실패하는 상태를 만들지 않습니다. Redis가 없어도 기존 Access 검증은 DB를 사용합니다.
다만 **증명 소비 자체에는 Redis가 필요**하므로 소비를 확인하지 못하면 비밀번호 변경을 시작하지 않습니다.

변경 커밋 전에 이미 인증·인가를 마친 일반 요청을 소급 취소하지 않습니다.
이전에 시작한 로그인/갱신의 성공 응답이 변경 이후 도착할 수 있지만 그 응답의 이전 버전 토큰은 사용할 수 없습니다.
새 비밀번호로 변경 이후 시작한 로그인은 새 버전으로 발급됩니다.
일반 로그아웃은 인증 버전을 증가시키지 않아 기존의 [Access Token 잔여 수명 정책](logout.md)을 유지합니다.

## 실패·중복·동시 요청·응답 유실

| 단계/상황 | 결과와 재시도 |
| --- | --- |
| 입력 검증·기존 비밀번호 등록 여부·현재 인증 버전·해시 생성 실패 | 증명 소비 전 거절. 입력/계정 상태를 먼저 해결 |
| 잘못된 목적·회원·이메일·버전의 증명 | 소비 없이 거절. 가입/탈퇴용 증명은 비밀번호 변경에 사용 불가 |
| Redis 소비 결과 불확실 | 503 EMAIL_VERIFICATION_UNAVAILABLE. 이 요청의 DB 변경은 시작하지 않음. 증명은 이미 소비됐을 수 있어 자동 재전송 대신 새 본인 확인 |
| 같은 증명의 동시 요청 | Lua에서 한 요청만 소비. 나머지는 이미 소비한 증명/현재 버전 오류로 거절 |
| 재발급 등으로 서로 다른 증명을 가진 동시 변경 | 회원 쓰기 잠금과 예상 버전 비교로 같은 버전에서는 한 변경만 커밋. 패자는 409 또는 401. 소비한 증명은 복원하지 않음 |
| 소비 뒤 회원·이메일·버전 충돌 | 409 PASSWORD_CHANGE_CONFLICT. 재로그인 후 새 본인 확인 필요 |
| 소비 뒤 DB 잠금·연결·커밋 오류 | 503 PASSWORD_CHANGE_UNAVAILABLE. 증명 복원 없음. 커밋 여부가 불확실할 수 있어 Refresh 쿠키를 만료시키고 로컬 로그인 상태 정리·재로그인으로 결과 확인 |
| 204 응답 유실 | 변경은 완료됐을 수 있음. 같은 요청을 자동 재전송하거나 소비한 증명을 되살리지 않음. 기존 토큰의 401만으로 어느 요청이 성공했는지는 알 수 없으므로 새 비밀번호 로그인으로 확인 |

DB 오류·응답 유실 후 새 비밀번호로 로그인되면 변경된 상태입니다. 새 비밀번호가 실패하면
연결 장애와 입력 오류를 구분한 뒤 이전 비밀번호로 로그인 가능한지 확인하고, 필요한 경우 새 본인 확인부터 진행합니다.
별도의 작업 결과 조회·멱등 응답 재생 API는 없습니다. 서비스에서 자동 재시도하지 않습니다.
Redis 연결 단절/드라이버 재전송이 정확히 한 번 실행을 보장한다는 뜻도 아닙니다.

## 오류 명세

공통 `application/problem+json`, `code`, `Cache-Control: no-store` 형식을 사용합니다.

| HTTP | code |
| --- | --- |
| 400 | INVALID_INPUT, PASSWORD_POLICY_VIOLATION, ACCOUNT_PROOF_INVALID 또는 Spring 요청 오류 |
| 401 | UNAUTHENTICATED(필터), AUTHENTICATION_REVOKED(서비스 확인 중 버전 변경) |
| 403 | INVALID_CSRF_TOKEN, ACCESS_DENIED, ACCOUNT_VERIFICATION_NOT_ALLOWED, PASSWORD_CHANGE_NOT_ALLOWED |
| 409 | ACCOUNT_PROOF_ALREADY_USED, PASSWORD_CHANGE_CONFLICT |
| 410 | ACCOUNT_PROOF_EXPIRED. 해당 바인딩의 증명이 없는 경우 포함 |
| 503 | AUTHENTICATION_UNAVAILABLE, EMAIL_VERIFICATION_UNAVAILABLE, PASSWORD_CHANGE_UNAVAILABLE |
| 500 | INTERNAL_SERVER_ERROR. 예상하지 못한 매핑·프로그래밍 오류 |

503 중 `PASSWORD_CHANGE_UNAVAILABLE`에서만 현재 Refresh 쿠키를 삭제합니다.
그 이전 조회/증명 단계의 503은 쿠키를 지우지 않습니다. 401은 WWW-Authenticate: Bearer를 포함합니다.
필터 순서상 CSRF가 먼저 실패하면 인증 실패 대신 403이 반환될 수 있습니다.

## 적용·확인 범위

새 앱 실행 전에 기존 절차대로 `flywayInfo` → `flywayMigrate` → `flywayValidate`를 실행합니다.
이미 적용한 V1은 수정하지 않습니다. 버전 없는 기존 Access/Refresh Token은 재로그인이 필요하고,
이전 형식의 본인 확인 코드/증명도 새로 발급받아야 합니다. 이전·새 앱을 함께 제공하는 혼합 배포는 지원하지 않으므로
트래픽을 새 버전으로 일괄 전환해야 모든 인스턴스가 버전 검사를 적용합니다.

테스트 전용 MySQL·Redis를 사용하는 API 통합 검증을 추가했습니다.
실행 방법, 확인한 흐름과 남은 검증 범위는 [인증 테스트 안내](auth-testing.md)를 참고하세요.
