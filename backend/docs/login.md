# 이메일 로그인과 토큰 인증 (#30)

관련 이슈: [#30](https://github.com/Daebangg/sayroam/issues/30), 상위 [#24](https://github.com/Daebangg/sayroam/issues/24).
이메일·비밀번호 확인 → Access Token·Refresh 쿠키 발급 → Bearer 인증 → 내 정보 조회를 제공합니다.
Refresh Token의 교체·재사용 탐지·동시 요청 기준은 [갱신 문서](refresh-token.md)를 참고합니다.
현재 로그인 종료는 [로그아웃 문서](logout.md)를 참고합니다. 구현 상태와 전체 인증 기능 검증 완료는 구분합니다.
가입은 기존 [이메일 회원가입](signup.md)을 사용하며 자동 로그인하지 않습니다.
이메일 로그인은 자격 정보를 확인한 뒤 공통 `LoginTokenService`로 신규 토큰을 발급합니다.
소셜 로그인도 회원 확인·가입 정책을 처리한 뒤 같은 발급 서비스를 사용하도록 기반을 분리했습니다.

## 실행 설정

로그인과 갱신에는 MySQL과 Redis가 필요합니다. 보호 API의 Access Token 검증은 MySQL을 사용하며 Redis나 SMTP를 호출하지 않습니다.
가입과 이메일 인증에는 기존의 Redis·SMTP 조건이 별도로 적용됩니다.

`backend/config/application-local.properties`에 아래 키를 추가합니다. 이 개인 파일은 Git에서 제외됩니다.

```properties
app.auth.access-token.secret-base64=<openssl rand -base64 32 로 생성한 값>
app.auth.access-token.issuer=sayroam
app.auth.access-token.audience=sayroam-api
app.auth.access-token.ttl-seconds=900
```

`secret-base64`는 Base64로 인코딩한 최소 32바이트의 암호학적 난수입니다. 문자열 비밀번호를 사용하지 않습니다.
누락·잘못된 Base64·32바이트 미만이면 시작을 거부합니다. 저장소의 공용 기본 키나 자동 임시 키는 없습니다.
키 생성 결과는 개인 파일·운영 비밀 저장소에만 보관하고 로그·문서·Git에 넣지 않습니다.
Spring Boot 환경변수 이름은 `APP_AUTH_ACCESSTOKEN_SECRETBASE64`, `APP_AUTH_ACCESSTOKEN_ISSUER`,
`APP_AUTH_ACCESSTOKEN_AUDIENCE`, `APP_AUTH_ACCESSTOKEN_TTLSECONDS`입니다.

TTL은 60~900초이며 기본 900초입니다. 모든 앱 인스턴스에서 같은 키·issuer·audience를 사용합니다.
현재는 단일 HS256 키만 지원합니다. 키 교체 후 기존 키로 발급한 Access Token은 인증되지 않습니다.
유효한 Refresh 쿠키·Redis 상태가 있으면 새 키로 서명한 Access Token을 갱신받을 수 있습니다.
이전 키를 함께 유지하는 무중단 키 회전은 이번 단계에 포함하지 않습니다.

## 로그인

1. `GET /api/auth/csrf`를 호출해 `XSRF-TOKEN` 쿠키를 받습니다.
2. 같은 쿠키와 원문 값을 복사한 `X-XSRF-TOKEN` 헤더로 로그인합니다. Authorization 헤더는 보내지 않습니다.

```http
POST /api/auth/login
Content-Type: application/json
X-XSRF-TOKEN: <XSRF-TOKEN 쿠키 원문>
Cookie: XSRF-TOKEN=<같은 원문>
```

```json
{
  "email": "user@example.com",
  "password": "replace-with-your-password"
}
```

이메일은 필수 ASCII 이메일·최대 254자이며, 앞뒤 공백을 제거한 뒤 전체 소문자로 조회합니다.
가입과 같은 정규화이며 Gmail 점 제거·플러스 주소 병합은 하지 않습니다.
비밀번호는 필수 8~128자이며 공백 제거·소문자화 등 정규화를 하지 않습니다.
형식이 유효한 요청에서 없는 이메일·잘못된 비밀번호·탈퇴·이메일 미인증·비밀번호 없는 소셜 계정은
모두 **401 INVALID_CREDENTIALS**, `Email or password is incorrect`로 응답합니다.
없는 계정도 실제 계정과 같은 인코더의 더미 해시를 검증합니다. 정확한 응답 시간의 동일함을 보장하지는 않습니다.
로그인 시도 횟수 제한·잠금 정책은 이번 단계에 추가하지 않습니다.

성공하면 **200 OK**, `Cache-Control: no-store`와 아래 필드를 반환합니다.

```json
{
  "accessToken": "<실제 서명된 JWT>",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

같은 응답의 `Set-Cookie`로 Refresh Token을 전달합니다. `sayroam_refresh`는 HttpOnly·SameSite=Strict·
Path=/api/auth·기본 Secure이며 Domain을 지정하지 않는 호스트 전용 쿠키입니다.
Refresh 원문은 JSON 응답에 넣지 않습니다. 비밀번호·서명 키도 응답에 포함하지 않습니다.
Redis에는 로그인별 회원 ID·토큰 해시·절대 만료·교체 횟수·사용 이력만 보관하며 HttpSession은 사용하지 않습니다.
Redis 저장이 실패하면 로그인 성공 응답과 Refresh 쿠키를 반환하지 않습니다.
요청·응답·포트 결과 객체의 문자열 표현은 비밀번호·토큰을 가립니다.

## 보호 API와 내 정보

브라우저에서는 Access Token을 JavaScript 메모리에 보관하고 아래 헤더로만 전달합니다.
쿠키·URL 쿼리·요청 본문은 Access Token 인증 수단으로 사용하지 않습니다.
새로고침·탭 재개·Access Token 만료 후에는 유효한 Refresh 쿠키로 갱신받습니다.
HTTPS로 전달하고 URL·로그에 토큰을 넣지 않습니다. 자세한 요청 순서는 [갱신 문서](refresh-token.md)를 참고합니다.

```http
GET /api/users/me
Authorization: Bearer <accessToken>
```

성공하면 **200 OK**, `Cache-Control: no-store`와 다음 회원 정보를 반환합니다.

```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "여행자",
  "profileImageUrl": null,
  "gender": null,
  "birthYear": null,
  "role": "USER"
}
```

선택 정보의 null과 `PATCH /api/users/me` 부분 수정 계약은 [프로필 문서](user-profile.md)를 따릅니다.

요청에서 다른 회원의 ID를 받지 않습니다. Spring 표준 Bearer 필터와 Nimbus 구현으로 HS256 서명을 검증하고,
`typ=at+jwt`, `token_use=access`, `iss`, `aud`, 필수 `exp`·`iat`·`jti`·양수 회원 ID `sub`를 확인합니다.
`auth_ver` 문자열 클레임도 필수이며 DB의 현재 인증 버전과 다르면 401로 거절합니다.
발급 토큰에는 이메일·닉네임·권한을 넣지 않습니다. 시간 검증의 허용 오차는 30초입니다.
만료 이후 최대 30초가 허용될 수 있으며, 미래 `iat`는 30초를 넘으면 거절합니다.

서명과 클레임 검증을 통과한 요청마다 MySQL에서 회원의 `deletedAt`과 현재 `role`을 조회합니다.
삭제·탈퇴 상태가 커밋된 후 시작하는 인증 요청은 기존 토큰도 401로 거절합니다.
이미 인증을 마친 진행 중 요청을 소급 취소하지는 않습니다. 권한 변경도 다음 인증 조회부터 적용됩니다.
토큰에 임의로 추가한 role/scope 클레임은 권한으로 사용하지 않습니다.
`/api/admin`과 하위 경로는 기존 ADMIN 접근 정책을 유지하며 일반 회원은 403입니다.
관리자 업무 API 자체는 이번 단계에서 추가하지 않습니다.

`POST /api/auth/logout`은 현재 로그인에 속한 Refresh를 폐기하고 쿠키를 만료시킵니다.
Access Token의 개별 차단 목록은 사용하지 않습니다. 클라이언트에서 토큰을 지워도 다른 곳에 남은 복사본은
만료(+허용 오차)까지 사용 가능합니다. 탈퇴·계정 삭제·서명 키 교체는 이와 별도로 접근을 차단합니다.
갱신 토큰 재사용을 탐지하면 해당 로그인은 더 이상 갱신할 수 없습니다. 이미 발급한 Access Token을
Redis에서 개별 차단하지는 않으므로 만료(+최대 30초 허용 오차)까지 접근 가능합니다.
클라이언트는 로그아웃 시 메모리의 Access Token·회원 정보를 제거하고 진행 중 갱신 결과를 폐기해야 합니다.
[비밀번호 변경](password-change.md)은 해시 변경과 인증 버전 증가를 같은 DB 트랜잭션으로 처리해
모든 기기의 기존 Access/Refresh Token을 차단합니다. 일반 로그아웃의 잔여 수명 정책과 구분합니다.
[회원 탈퇴](account-deletion.md)는 같은 본인 확인 기반에서 탈퇴 상태와 프로필·로그인 수단을 정리하고,
재가입하더라도 기존 회원의 토큰이나 공유 기록을 새 회원에게 이어 붙이지 않습니다.

## CSRF 범위

`/api/auth`와 하위 경로의 POST·PUT·PATCH·DELETE 등 안전하지 않은 메서드는
`XSRF-TOKEN` 쿠키와 `X-XSRF-TOKEN` 헤더가 필요합니다. 로그인·이메일 인증·가입·갱신·로그아웃 경로를 포함합니다.
Bearer 헤더를 추가해도 이 경로의 CSRF 검사는 생략하지 않습니다. 후속 쿠키 인증 경로도 이 namespace를 사용해야 합니다.
`GET /api/auth/csrf`는 인증 없이 204를 반환합니다.
그 외 보호 API는 헤더 전용 Bearer 인증을 사용하므로 CSRF 검사를 요구하지 않습니다.
세션·쿠키·Basic 인증으로 보호 API에 접근할 수 없습니다.

표준 Bearer 필터를 직접 연결해 Spring Resource Server 설정의 자동 CSRF 제외가 auth 경로를 우회하지 않게 합니다.
위조·만료된 Bearer 헤더를 로그인·가입·이메일 인증 API에 함께 보내면 인증 필터에서 401로 거절될 수 있습니다.
`POST /api/auth/refresh`와 `/api/auth/logout`은 Authorization 헤더를 무시하고 Refresh 쿠키를 사용합니다.
로그아웃은 Refresh 쿠키가 없어도 CSRF 검증 후 204로 브라우저 정리를 허용합니다.
Swagger Authorize에는 Access Token 원문만 입력합니다. 로그인 호출 전에는 Authorize를 해제하고
CSRF 쿠키·헤더를 준비합니다. Swagger의 인증 입력은 CSRF 헤더를 대신하지 않습니다.

## 오류

| HTTP | code | 의미 |
| --- | --- | --- |
| 400 | INVALID_INPUT / BAD_REQUEST | 이메일·비밀번호 형식 또는 JSON 오류 |
| 401 | INVALID_CREDENTIALS | 계정 조회·비밀번호 확인 실패. 계정 존재 여부 미공개 |
| 401 | UNAUTHENTICATED | Bearer 누락·잘못된 형식·서명·만료·클레임, 삭제·탈퇴 회원. WWW-Authenticate: Bearer |
| 401 | REFRESH_TOKEN_INVALID / REFRESH_TOKEN_REUSED | 갱신 실패. Refresh 쿠키 만료, 재로그인 필요 |
| 403 | INVALID_CSRF_TOKEN | auth 변경 요청의 CSRF 쿠키·헤더 누락·변조 |
| 403 | ACCESS_DENIED | 현재 회원의 권한 부족 |
| 503 | AUTHENTICATION_UNAVAILABLE | DB 또는 Refresh Redis 장애. 갱신 결과가 불확실할 수 있으므로 자동 재시도하지 않음 |
| 500 | INTERNAL_SERVER_ERROR | 예상하지 못한 매핑·서명 처리·프로그래밍 오류 |

응답은 기존 `application/problem+json` 형식입니다. 비밀번호 실패와 토큰 실패는 서로 다른 코드이며
없는 이메일과 잘못된 비밀번호 사이에서는 동일한 상태·코드·문구를 반환합니다.

## 코드 의존성과 확인 범위

| 위치 | 책임 |
| --- | --- |
| auth/presentation/LoginController·LoginRequest·LoginResponse | 입력 검증·HTTP 응답·API 명세 |
| auth/application/LoginService | 정규화 → 계정 조회 → 비밀번호 검증 → 토큰 발급 |
| auth/application/RefreshTokenService·model/TokenPair | 갱신 확인·회원 상태 조회·서명·원자적 교체, 전송 방식과 독립적인 결과 |
| auth/application/LogoutService·auth/presentation/LogoutController | 현재 로그인 폐기 포트 호출과 HTTP 204·쿠키 만료 |
| auth/application/port/out/AccountLookup·PasswordHasher·AccessTokenIssuer·RefreshTokenStore | 외부 조회·검증·발급·갱신 상태 계약 |
| auth/infrastructure/persistence/JpaAccountLookup | 읽기 트랜잭션·필요한 필드 조회·알려진 DB 장애 변환 |
| auth/infrastructure/security/SpringPasswordHasher·JwtAccessTokenIssuer | 기존 인코더의 검증과 JWT 서명 |
| auth/infrastructure/config/AccessTokenConfig·AccessTokenProperties | 외부 키 검증·JWT 검증 정책 |
| auth/infrastructure/redis/RedisRefreshTokenStore·redis/refresh-token-*.lua | 로그인별 상태·절대 만료·교체·재사용 탐지 |
| auth/presentation/RefreshTokenController·cookie/RefreshTokenCookie·config/RefreshCookieProperties | 갱신 HTTP·쿠키·브라우저 전달 정책 |
| global/security/SecurityConfig·ActiveAccountJwtConverter·SecurityProblemHandler | 필터 연결·활성 회원 및 권한 연결·공통 오류 |
| user/presentation/CurrentUserController·CurrentUserResponse | 인증 때 조회한 현재 회원 정보 응답 |

Application은 JPA·Spring Security·JWT 라이브러리·HTTP 타입을 직접 참조하지 않습니다.
기존 user 엔티티와 스키마를 유지하며 auth에 별도 회원 엔티티를 만들지 않습니다.

테스트 전용 MySQL·Redis를 사용하는 API 통합 검증을 추가했습니다.
실행 방법, 확인한 흐름과 남은 검증 범위는 [인증 테스트 안내](auth-testing.md)를 참고하세요.

참고: [Spring JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html),
[Spring CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).
