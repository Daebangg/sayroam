# 이메일 인증 후 회원가입 (#28)

현재 흐름은 CSRF 쿠키 준비 → 이메일 코드 발급 → 코드 검증 → 가입용 증명 발급 → 회원가입입니다.
가입 성공은 계정 생성만 의미하며 로그인·JWT·Refresh 처리는 #30 범위입니다.

## 실행 조건과 요청

회원가입에는 MySQL과 Redis가 모두 필요합니다. 이미 받은 유효한 증명이 있다면 SMTP가 꺼져 있어도
가입할 수 있습니다. 새 증명을 얻으려면 실제 SMTP를 통한 [이메일 인증](email-verification.md)이 필요합니다.
기존 Docker·외부 application-local.properties 설정을 그대로 사용하고 새 비밀값이나 환경변수는 추가하지 않습니다.

`POST /api/auth/signup`은 인증 없이 호출할 수 있지만 `XSRF-TOKEN` 쿠키와 그 원문을 담은
`X-XSRF-TOKEN` 헤더가 필요합니다. Swagger Authorize는 이 헤더를 대신하지 않습니다.

```http
POST /api/auth/signup
Content-Type: application/json
X-XSRF-TOKEN: <동일한 브라우저의 XSRF-TOKEN 쿠키 값>
Cookie: XSRF-TOKEN=<동일한 값>
```

```json
{
  "email": "user@example.com",
  "password": "ReplaceWithYours1!",
  "nickname": "여행자",
  "signupProof": "코드 검증 응답에서 받은 실제 43자 증명으로 교체"
}
```

위 값은 설명용이며 실제 이메일·비밀번호·검증 응답을 사용합니다. Swagger의 기본 예시 값은 발급 결과가 아닙니다.
증명이나 비밀번호를 URL·로그·공유 스크린샷에 넣지 않습니다. 요청 DTO의 문자열 표현도 모든 값을 가립니다.

| 입력 | 규칙 |
| --- | --- |
| email | 필수 ASCII 이메일, 최대 254자. 앞뒤 공백 제거 후 전체 소문자로 정규화 |
| password | 필수 8~24자·대문자·숫자·특수문자 각 1개 이상·공백/제어/숨김 문자 금지. 정규화하지 않음 |
| nickname | 앞뒤 공백 제거 후 필수 1~50자, 제어·숨김·줄바꿈 문자 제외 |
| signupProof | 코드 검증 API가 반환한 Base64URL 43자 난수 원문 |

이메일 비교는 발급·검증·가입에 동일하게 적용합니다. Gmail 점 제거·플러스 주소 병합은 하지 않습니다.
일반 회원 권한은 서버에서 `USER`로 고정하며 role·인증 시각·비밀번호 해시를 입력받지 않습니다.

성공 시 DB 커밋을 마친 뒤 **201 Created**, `Cache-Control: no-store`와 아래 응답을 반환합니다.

```json
{
  "userId": 1
}
```

로그인 토큰·비밀번호·증명은 반환하지 않으며 Location 헤더도 생성하지 않습니다. 로그인 후 `GET /api/users/me`로 본인 정보를 조회할 수 있습니다.

## 증명 소비와 DB 저장

1. Presentation에서 입력을 검증합니다. 형식 오류·CSRF 실패는 증명을 소비하지 않습니다.
2. Application이 공통 도메인 `PasswordPolicy`로 비밀번호를 먼저 검사한 뒤 이메일을 정규화하고 증명 원문의 SHA-256 다이제스트를 만듭니다.
3. Redis Lua에서 해당 이메일 키의 TTL·purpose=signup·다이제스트·available 상태·인증 시각을 확인합니다.
4. 같은 Lua에서 상태를 consumed로 바꾸고 기존 TTL은 유지합니다. 동일 증명을 동시에 소비하는 요청 중 하나만 통과합니다.
5. 통과한 요청만 비밀번호를 해시합니다. 입력 비밀번호·증명 원문은 Redis나 MySQL에 저장하지 않습니다.
6. DB 트랜잭션에서 User와 UserCredential을 함께 저장하고 커밋합니다.

`users.email`의 기존 `uk_users_email` UNIQUE 제약이 최종 중복 판단 기준입니다. 사전 exists 조회만으로
동시 가입을 제어하지 않습니다. 서로 다른 증명을 얻은 요청끼리 경쟁해도 같은 이메일의 계정은 하나만 저장됩니다.
회원 생성이 실패하면 같은 트랜잭션의 비밀번호 저장도 롤백됩니다. V1 마이그레이션은 수정하지 않습니다.

`emailVerifiedAt`에는 가입 시각이 아니라 코드 검증 Lua가 기록한 Redis 서버의 인증 시각을 UTC로 변환해 저장합니다.
회원·비밀번호 저장 뒤에는 Redis를 다시 호출하지 않아 후속 Redis 정리 실패가 DB 가입 성공을 실패로 바꾸지 않습니다.

## 실패 시 복구·재시도 정책

Redis 소비와 MySQL 저장은 하나의 분산 트랜잭션이 아닙니다. 이번 정책은 **증명을 먼저 소비하고 자동 복구하지 않는 것**입니다.
예약 해제·임대 만료·보상 재시도로 증명을 되살리는 처리는 도입하지 않습니다.

| 상황 | 저장 상태와 다음 행동 |
| --- | --- |
| 입력·CSRF 오류 | 소비 전 실패. 입력·헤더를 수정하고 같은 유효 증명으로 재시도 가능 |
| 다른 이메일·틀린 증명 | 해당 요청으로 유효 증명을 소비하지 않음. 올바른 이메일·증명 사용 |
| 만료·재발급으로 사라진 증명 | 새 이메일 인증 필요 |
| 이미 소비한 증명 | 재사용 불가. 이전 가입이 성공했을 수 있으므로 결과를 확인하고, 가입이 필요하면 새 인증 |
| 이메일 UNIQUE 충돌 | 기존 계정 유지. 이번 증명은 소비된 상태. 로그인은 #30 구현 후 가능 |
| 증명 소비 후 DB 연결·잠금 장애 | DB 트랜잭션 롤백을 시도하지만 증명은 복구하지 않음. 장애 복구 후 새 인증 필요 |
| Redis 소비 응답 유실 | 소비됐는지 불확실할 수 있음. DB 저장을 진행하지 않으며 자동 재시도 없음 |
| DB 커밋 응답 또는 HTTP 성공 응답 유실 | 계정이 이미 생성됐을 수도 있음. 증명을 복구하거나 가입을 자동 재시도하지 않음 |

이 방식은 장애 시 사용자가 이메일 인증을 다시 해야 한다는 비용이 있습니다. 저장 실패 후 인증 상태를
자동 복구하는 편의 기능은 제공하지 않습니다. 동일 증명으로 성공 응답을 재현하는 멱등 응답 기능도 없습니다.
재발급은 기존 발송 제한·재발송 대기 정책을 그대로 따릅니다.
동시에 새 코드가 발급되면 Redis가 먼저 처리한 순서에 따릅니다. 재발급이 먼저면 이전 증명은 무효화되고,
증명 소비가 먼저면 이미 승인된 가입은 계속 진행하며 DB UNIQUE 제약으로 최종 중복 저장을 방지합니다.

## 오류 응답

업무·인프라 실패는 공통 `BusinessException`을 상속한 `AuthException`으로 전달합니다.
`AuthErrorCode`가 공통 `ErrorCode` 계약으로 실패 종류·코드·공개 문구를 정의하고, `ApiProblems`가 실패 종류를 HTTP 상태로 변환합니다.
인증 예외와 요청 검증·JSON 오류, 예상하지 못한 버그는 `GlobalExceptionHandler`가 처리하며,
CSRF 실패는 SecurityProblemHandler가 처리합니다. 내부 SQL·접속 정보·원본 예외는 응답에 포함하지 않습니다.

| HTTP | code | 의미 |
| --- | --- | --- |
| 400 | INVALID_INPUT / BAD_REQUEST | 필수값·길이·형식 또는 JSON 오류 |
| 400 | PASSWORD_POLICY_VIOLATION | 서비스 직접 호출에도 적용하는 8~24자·대문자·숫자·특수문자·공백/제어/숨김 문자 금지 정책. 증명 소비 전 거절 |
| 400 | SIGNUP_PROOF_INVALID | 해당 이메일의 증명 다이제스트·목적·상태 불일치 |
| 403 | INVALID_CSRF_TOKEN | CSRF 쿠키·헤더 누락·변조 |
| 409 | SIGNUP_PROOF_ALREADY_USED | 같은 증명 재사용 |
| 409 | EMAIL_ALREADY_REGISTERED | DB 이메일 UNIQUE 충돌 |
| 410 | SIGNUP_PROOF_EXPIRED | 해당 이메일의 증명이 없거나 만료됨 |
| 503 | EMAIL_VERIFICATION_UNAVAILABLE | Redis 접근 실패, 소비 여부가 불확실할 수 있음 |
| 503 | SIGNUP_UNAVAILABLE | DB 연결·잠금·커밋 실패, 커밋 결과가 불확실할 수 있음 |
| 500 | INTERNAL_SERVER_ERROR | 예상하지 못한 매핑·무결성·프로그래밍 오류 |

다른 이메일의 증명을 제출했을 때 그 이메일의 증명 키 자체가 없다면 410입니다.
DB 실패 응답에 Retry-After를 제공하지 않습니다. 일정 시간 기다린다고 소비된 증명이 복구되지는 않습니다.

## 코드 구조

| 위치 | 책임 |
| --- | --- |
| auth/application/service/SignupService | 비밀번호 정책 확인 → 증명 결과 판단 → 비밀번호 해시 → 계정 생성 포트 호출 |
| auth/domain/PasswordPolicy | 가입·비밀번호 변경 공통 정책. 원문을 정규화하거나 보관하지 않음 |
| auth/application/port/out/EmailVerificationStore | 이메일 인증 상태와 가입 증명 소비 계약 |
| auth/application/port/out/PasswordHasher | 외부 인코더와 독립적인 비밀번호 해시 계약 |
| auth/application/port/out/AccountRegistration | 일반 회원·비밀번호를 함께 커밋하는 계약 |
| auth/infrastructure/redis/RedisEmailVerificationStore | Lua 기반 원자적 증명 소비 |
| auth/infrastructure/security/SpringPasswordHasher | Spring PasswordEncoder 호출 |
| auth/infrastructure/persistence/JpaAccountRegistration | JPA 저장·DB 트랜잭션·이메일 충돌 및 알려진 장애 변환 |
| global/config/PasswordConfig | 공통 비밀번호 인코더 설정. 후속 로그인에서 재사용 |
| user/domain/entity/User, UserCredential | 영속 회원과 비밀번호 해시. 일반 회원 생성 시 인증 시각 기록 |
| auth/presentation/controller/SignupController | 요청 검증과 201 응답, Swagger 명세 |
| auth/application/error/AuthErrorCode, AuthException | 공통 오류 계약에 따른 인증 실패 종류·공개 문구와 예외·추가 정보 |
| global/exception/ApiProblems, GlobalExceptionHandler | 공통 HTTP 변환과 MVC 오류 응답·인증 전용 헤더·쿠키 처리 |

Application은 JPA·Redis·SMTP·Spring Security 인코더를 직접 참조하지 않습니다.
회원 모델은 기존 user 도메인에 유지하고 auth에 별도 회원 엔티티를 만들지 않습니다.

비밀번호는 Spring의 PBKDF2-HMAC-SHA256, 600,000회 반복, 16바이트 랜덤 salt, 256비트 해시를 사용합니다.
저장 형식은 `{pbkdf2-sha256-v1}` 접두사와 salt+해시이며 user_credential.password_hash의 기존 255자 길이에 들어갑니다.
같은 접두사의 파라미터는 이후 변경하지 않고 새 버전을 추가해야 기존 계정의 검증을 유지할 수 있습니다.
선택 근거: [Spring PasswordEncoder](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html),
[OWASP PBKDF2 반복 횟수](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#pbkdf2).
추가 암호화 라이브러리나 운영 비밀값을 도입하지 않고 기존 Spring Security와 JDK로 구성합니다.

## 확인 범위

테스트 전용 MySQL·Redis를 사용하는 API 통합 검증을 추가했습니다.
실행 방법, 확인한 흐름과 남은 검증 범위는 [인증 테스트 안내](auth-testing.md)를 참고하세요.
