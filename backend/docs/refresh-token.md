# Refresh Token 발급·갱신 (#30 2단계)

관련 이슈: [#30](https://github.com/Daebangg/sayroam/issues/30), 전체 인증 검증 [#24](https://github.com/Daebangg/sayroam/issues/24).
[로그인](login.md)의 Access Token 발급·회원 인증에 Refresh 쿠키와 갱신을 연결합니다.
현재 로그인 종료는 [로그아웃 문서](logout.md)를 참고합니다. 코드 구현과 실제 시나리오 검증 완료를 구분합니다.

## 실행 설정과 토큰 전달

로그인·갱신은 MySQL과 Redis를 모두 사용합니다. Access Token으로 보호 API에 접근할 때는 Redis를 사용하지 않습니다.
새 비밀값·DB 테이블은 추가하지 않고 기존 서명 키와 Redis 연결 설정을 사용합니다.
배포 전에 `V2__add_user_auth_version.sql`로 `users.auth_version`을 추가해야 합니다.
미적용 상태에서는 로그인·갱신·Bearer 인증을 정상 처리할 수 없습니다.
인증 버전이 없는 이전 형식의 Access Token·Refresh Token은 인증·갱신에 사용할 수 없으므로 재로그인이 필요합니다.

| 설정 | 기본값 | 허용 범위·의미 |
| --- | --- | --- |
| app.auth.refresh-token.ttl-seconds | 604800 | 3600~2592000초. 로그인 시점부터 7일의 절대 유효기간 |
| app.auth.refresh-token.max-rotations | 10000 | 1~10000회. 로그인별 교체 한도와 사용 이력 저장량 제한 |
| app.auth.refresh-cookie.secure | true | HTTPS 쿠키. HTTP local 프로필에서는 false |

운영은 local 프로필 없이 HTTPS와 Secure=true를 사용합니다. 공용 local 프로필은 HTTP 개발을 위해
Secure=false를 설정하며 HttpOnly·SameSite·Path는 그대로 유지합니다.
Spring Boot 환경변수는 `APP_AUTH_REFRESHTOKEN_TTLSECONDS`, `APP_AUTH_REFRESHTOKEN_MAXROTATIONS`,
`APP_AUTH_REFRESHCOOKIE_SECURE`입니다. Docker Redis 포트와 앱 포트는 별도 설정이므로 동일하게 맞춥니다.

Refresh Token 원문은 `로그인 UUID.43자 Base64URL 난수` 형식입니다. 비밀 부분은 SecureRandom 32바이트이며
JWT가 아닙니다. UUID만으로 갱신할 수 없습니다. 요청 원문·해시를 로그나 URL에 넣지 않습니다.

로그인 성공 응답의 JSON은 기존 `accessToken`, `tokenType`, `expiresIn` 필드를 유지합니다.
같은 응답의 Set-Cookie로만 Refresh 원문을 전달합니다.

```http
Set-Cookie: sayroam_refresh=<서버가 발급한 값>; Path=/api/auth; Max-Age=604800; HttpOnly; Secure; SameSite=Strict
Cache-Control: no-store
```

Domain은 지정하지 않습니다. 브라우저가 발급한 호스트에만 쿠키를 전송하며 JavaScript에서 원문을 읽을 수 없습니다.
Access Token은 JavaScript 메모리에 보관하고 보호 API에 Authorization 헤더로만 전달합니다.
동일 출처 브라우저 사용이 기준이며 교차 출처 CORS·교차 사이트 쿠키 정책은 이번 단계에서 추가하지 않습니다.
`localhost`, `127.0.0.1`, `[::1]`은 쿠키 호스트가 다르므로 로그인·CSRF·갱신에 같은 주소를 사용합니다.

## POST /api/auth/refresh

1. 같은 브라우저의 `GET /api/auth/csrf`로 CSRF 쿠키를 준비합니다. 유효한 쿠키가 이미 있으면 재사용합니다.
2. `sayroam_refresh`, `XSRF-TOKEN` 쿠키와 `X-XSRF-TOKEN` 헤더로 호출합니다.
3. 새 Access Token은 메모리에서 교체하고 Refresh 쿠키 교체는 브라우저에 맡깁니다.

```http
POST /api/auth/refresh
X-XSRF-TOKEN: <동일한 브라우저의 XSRF-TOKEN 쿠키 원문>
Cookie: XSRF-TOKEN=<같은 원문>; sayroam_refresh=<로그인 응답으로 받은 쿠키>
```

본문과 Access Token은 필요하지 않습니다. 갱신·로그아웃 경로는 Authorization 헤더를 인증에 사용하지 않으므로
만료·변조된 Access Token이 함께 전달되어도 쿠키·CSRF 검증 결과로 처리합니다.
Refresh 원문을 헤더·JSON·쿼리로 대신 전달할 수 없습니다. 같은 이름의 Refresh 쿠키가 여러 개면 거절합니다.
POST·쿠키 경로의 CSRF 검사는 Bearer 헤더가 있더라도 생략하지 않습니다.

성공은 **200 OK**, `Cache-Control: no-store`, 새 Access Token 본문과 교체된 `Set-Cookie`입니다.
응답 본문 스키마는 로그인과 같은 `LoginResponse`입니다.
Refresh 쿠키의 Max-Age는 Redis에 남은 절대 유효기간의 초 단위 내림값이며, 갱신으로 만료를 연장하지 않습니다.
남은 시간이 1초 미만이거나 교체 한도에 도달하면 갱신하지 않고 재로그인이 필요합니다.
로그인·갱신·로그아웃은 현재 CSRF 쿠키를 유지하며, 삭제·만료됐다면 `/api/auth/csrf`로 다시 준비합니다.

Swagger에서는 같은 호스트에서 로그인해 브라우저가 Refresh 쿠키를 저장한 다음 CSRF 헤더를 입력합니다.
`refreshCookie` 보안 명세는 자동 전송 쿠키를 설명하는 용도이며 Authorize에 원문을 수동 입력하는 방식이 아닙니다.
Swagger의 인증 입력은 CSRF 헤더를 대신하지 않습니다.

## 저장·교체와 재사용 탐지

로그인마다 독립적인 UUID를 만듭니다. Redis 키는 `sayroam:auth:refresh:{UUID}`이며 다음을 저장합니다.

- userId, authVersion, status(active/revoked), currentDigest(토큰 전체 원문의 SHA-256)
- expiresAtEpochMillis, Redis TTL, rotations, maxRotations
- used:<이전 토큰 다이제스트> 사용 이력

원문 토큰·비밀번호·이메일은 Redis 상태에 넣지 않습니다. 한 로그인 내 작업은 한 키의 Lua로 처리합니다.
상태와 사용 이력은 최초 절대 만료까지 유지하며 교체와 폐기 시 TTL을 연장하지 않습니다.
저장소 쓰기 권한은 인증 권한과 연결되므로 운영 Redis의 접근 정책은 기존 비밀 설정·네트워크 정책을 따릅니다.
로컬 Docker Redis가 재시작으로 상태를 잃으면 기존 Refresh 쿠키는 거절되며 재로그인이 필요합니다.

갱신 흐름은 다음과 같습니다.

1. Application이 쿠키 원문 형식을 확인하고 다이제스트를 계산합니다.
2. inspect Lua가 TTL·active·현재 토큰을 확인하고 회원 ID를 반환합니다. 이미 사용한 토큰이면 해당 로그인을 폐기합니다.
3. 회원 조회 포트로 DB의 삭제·탈퇴 여부와 인증 버전을 확인합니다. 활성 회원이 없거나 Redis에 저장한 버전과 다르면 거절합니다.
4. Access Token을 서명하고 다음 Refresh 난수를 준비합니다. 아직 클라이언트에 전달하지 않습니다.
5. rotate Lua가 현재 토큰·TTL·active를 다시 확인한 뒤 이전 값을 used에 기록하고 다음 다이제스트로 교체합니다.
6. 교체가 확인된 뒤에만 Access Token과 새 Refresh 쿠키를 응답합니다.

과거에 실제 사용한 다이제스트가 다시 들어오면 현재 Refresh도 사용할 수 없도록 해당 로그인 상태를 revoked로 바꿉니다.
UUID만 알거나 임의 난수를 보낸 요청은 유효한 다른 로그인을 폐기하지 못합니다.
다른 기기·로그인의 UUID는 영향을 받지 않습니다. 새 로그인은 새 UUID를 만들며 기존 UUID를 자동으로 모두 폐기하지 않습니다.
단, [비밀번호 변경](password-change.md)은 DB 인증 버전을 증가시켜 모든 기기의 이전 버전 Refresh를 거절합니다.
버전 불일치 시 Redis 키를 즉시 삭제할 필요는 없으며 기존 절대 TTL로 정리합니다. 버전 없는 이전 형식도 갱신할 수 없습니다.
로그아웃도 현재 쿠키에 연결된 UUID만 폐기합니다. 갱신과 로그아웃이 경합하면 현재 또는 이전 토큰의
소유를 확인해 같은 로그인을 폐기하므로 이미 교체된 새 Refresh도 후속 갱신을 할 수 없습니다.

동일한 현재 토큰으로 동시에 갱신하면 **하나만 교체에 성공**하고, 뒤의 요청은 사용 이력을 발견해
그 로그인 전체의 후속 갱신을 차단합니다. 첫 성공 응답에 들어간 새 Refresh도 그 시점부터 사용할 수 없습니다.
이전 값 허용 유예·동일 응답 재현·중복 요청 멱등 성공은 제공하지 않습니다.
첫 재사용은 REFRESH_TOKEN_REUSED, 이미 폐기된 상태의 후속 요청은 REFRESH_TOKEN_INVALID입니다.

프론트는 한 번의 만료에 갱신 요청을 하나만 보내고 대기 중 API가 그 결과를 공유해야 합니다.
같은 쿠키를 공유하는 여러 탭의 갱신도 조정해야 합니다. 응답 유실 후 같은 쿠키로 자동 재시도하면
정상 클라이언트라도 재사용으로 폐기될 수 있으므로 재로그인으로 복구합니다.

갱신 권한 폐기는 이미 발급한 Access Token을 개별 차단하지 않습니다. Access Token은 만료(+최대 30초 오차)까지
사용할 수 있습니다. 회원 삭제·탈퇴는 기존 DB 상태 조회에 의해 다음 인증부터 차단됩니다.
DB 상태 조회 직후 탈퇴가 커밋되는 진행 중 요청을 소급 취소하지는 않으며, 뒤의 인증·갱신에서 거절합니다.

## 실패와 재시도

| 상황 | HTTP·code | 상태·다음 행동 |
| --- | --- | --- |
| 쿠키 없음·중복·형식 오류·만료·폐기·교체 한도·삭제/탈퇴 회원 | 401 REFRESH_TOKEN_INVALID | Refresh 쿠키 만료. 재로그인 |
| 이미 사용한 토큰 | 401 REFRESH_TOKEN_REUSED | 해당 로그인의 후속 갱신 폐기, 쿠키 만료. 재로그인 |
| CSRF 누락·변조 | 403 INVALID_CSRF_TOKEN | 필터에서 거절하며 갱신 상태와 Refresh 쿠키를 바꾸지 않음 |
| MySQL·Redis 연결/잠금/트랜잭션 장애 | 503 AUTHENTICATION_UNAVAILABLE | 성공 응답과 새 쿠키 없음. 현재 쿠키는 지우지 않지만 결과 불확실 시 재로그인 |
| 예상하지 못한 상태·매핑·서명 오류 | 500 INTERNAL_SERVER_ERROR | 내부 정보 미공개. 자동 갱신 재시도 없음 |

오류는 기존 `application/problem+json`과 `Cache-Control: no-store`를 사용합니다.
401의 쿠키 만료는 발급과 같은 이름·호스트·경로·속성으로 Max-Age=0을 설정합니다.
다른 경로·호스트의 중복 쿠키를 서버가 모두 삭제할 수는 없으므로 그러한 기존 쿠키는 브라우저에서 정리해야 합니다.
로그인 비밀번호 실패는 기존 Refresh 쿠키를 지우지 않습니다.

DB 조회나 서명이 교체 전에 실패하면 현재 Refresh를 소비하지 않습니다.
Redis inspect는 재사용을 발견했을 때만 상태를 폐기하며 정상 현재 토큰을 소비하지 않습니다.
Redis 교체 실행·응답 유실, HTTP 성공 응답 유실에서는 이전 토큰이 이미 소비됐을 수 있습니다.
소비된 이전 값을 복구하거나 원문을 Redis에서 꺼내지 않으며 자동 보상·재시도를 하지 않습니다.
세 저장소 작업과 HTTP 응답은 하나의 분산 트랜잭션이 아닙니다.

## 코드 의존성과 확인 범위

| 위치 | 책임 |
| --- | --- |
| auth/domain/RefreshToken | 난수·형식·다이제스트. JDK와 순수 도메인 코드만 사용 |
| auth/application/LoginService·RefreshTokenService | 로그인 저장·갱신 상태 확인·회원 조회·서명·교체 흐름 |
| auth/application/port/out/RefreshTokenStore | 저장·확인·원자적 교체·소유 확인 후 폐기 계약 |
| auth/application/dto/TokenPairResult | 쿠키·HTTP 타입이 없는 결과 |
| auth/infrastructure/persistence/RedisRefreshTokenStore | StringRedisTemplate·Lua 실행·저장소 장애 변환 |
| auth/infrastructure/persistence/RefreshTokenProperties | TTL·교체 한도 바인딩 |
| auth/presentation/controller/RefreshTokenController·cookie/RefreshTokenCookie·cookie/RefreshCookieProperties | 쿠키 추출·발급·삭제·HTTP와 브라우저 정책 |
| global/security/SecurityConfig | 갱신의 Access Token 제외와 기존 auth CSRF 검사 유지 |

Application은 Redis·JPA·JWT·Spring Security·Servlet·HTTP 타입이나 Infrastructure/Presentation을 참조하지 않습니다.
쿠키 설정도 Presentation에 두며 Application이나 Redis 어댑터에 전달하지 않습니다.

테스트 전용 MySQL·Redis를 사용하는 API 통합 검증을 추가했습니다.
실행 방법, 확인한 흐름과 남은 검증 범위는 [인증 테스트 안내](auth-testing.md)를 참고하세요.

참고: [Refresh Token 회전·재사용 탐지](https://datatracker.ietf.org/doc/html/rfc9700#section-4.14.2),
[Redis Lua 원자적 실행](https://redis.io/docs/latest/develop/programmability/eval-intro/),
[Spring ResponseCookie](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/ResponseCookie.ResponseCookieBuilder.html).
