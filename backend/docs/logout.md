# 현재 로그인 로그아웃 (#30 3단계)

관련 이슈: [#30](https://github.com/Daebangg/sayroam/issues/30), 전체 인증 검증 [#24](https://github.com/Daebangg/sayroam/issues/24).
[로그인](login.md)·[갱신](refresh-token.md)에 현재 로그인 종료를 연결합니다.
기존 RefreshTokenStore의 소유 확인·원자적 폐기를 재사용하며 새 비밀값·테이블·설정은 추가하지 않습니다.

## POST /api/auth/logout

```http
POST /api/auth/logout
X-XSRF-TOKEN: <같은 브라우저의 XSRF-TOKEN 쿠키 원문>
Cookie: XSRF-TOKEN=<같은 원문>; sayroam_refresh=<로그인 응답에서 받은 쿠키>
```

본문과 Access Token은 필요하지 않습니다. Refresh 쿠키가 자동 전송되며 서버는 그 쿠키로 종료 대상을 찾습니다.
이 경로는 Authorization 헤더를 무시하므로 Access Token 누락·만료·변조가 로그아웃을 가로막지 않습니다.
Refresh 쿠키를 헤더·본문·URL 쿼리의 토큰으로 대신 전달할 수 없습니다.

CSRF 쿠키·헤더는 Refresh 쿠키가 없어도 필수입니다. 쿠키가 없다면 먼저 `GET /api/auth/csrf`로 준비합니다.
기존 auth 경로와 같은 SPA 쿠키 → 헤더 검증을 사용하며 Bearer 헤더가 있어도 CSRF 검사를 생략하지 않습니다.

성공은 **204 No Content**, `Cache-Control: no-store`, 발급과 같은 이름·호스트·경로·속성의 쿠키 만료입니다.

```http
HTTP/1.1 204 No Content
Cache-Control: no-store
Set-Cookie: sayroam_refresh=; Path=/api/auth; Max-Age=0; HttpOnly; Secure; SameSite=Strict
```

Secure는 운영 기본값이며 HTTP local 프로필에서는 false입니다. Domain은 지정하지 않습니다.
현재 XSRF-TOKEN 쿠키는 유지해 다음 로그인에도 사용할 수 있습니다. CSRF 쿠키를 삭제·만료한 경우 다시 준비합니다.
Swagger에서는 같은 호스트의 쿠키를 자동 전송하고 X-XSRF-TOKEN 헤더를 입력합니다.
Refresh 쿠키는 선택 입력으로 명세에 표시되며, 브라우저의 HttpOnly 쿠키를 수동 입력할 필요는 없습니다.

## 저장소 폐기와 멱등 처리

1. Presentation이 Refresh 쿠키를 하나만 추출합니다. 같은 이름이 여러 개면 임의 선택 없이 401로 거절합니다.
2. Application이 형식을 확인하고 로그인 UUID와 토큰 다이제스트를 구합니다.
3. 정상 형식이면 RefreshTokenStore.revoke를 호출합니다. Redis Lua가 현재 값 또는 사용 이력의 소유를 확인합니다.
4. 일치하는 로그인 상태를 revoked로 바꾸고 currentDigest를 제거합니다. 기존 TTL과 사용 이력은 유지합니다.
5. 저장소 결과가 확인된 뒤 Presentation이 Refresh 쿠키를 만료시키고 204를 반환합니다.

현재 Refresh 토큰과 이미 교체된 이전 토큰 모두 해당 로그인을 종료할 수 있습니다. UUID만 알거나
임의의 비밀값을 제출한 요청은 다른 사람의 갱신 권한을 폐기하지 못합니다.
다른 기기·로그인의 UUID는 영향을 받지 않으며 모든 기기 로그아웃은 이번 범위에 포함하지 않습니다.

쿠키 없음·형식 오류는 Redis를 호출하지 않고 204로 브라우저 쿠키만 정리합니다.
정상 형식이지만 저장된 일치 토큰이 없거나 만료·이미 폐기된 상태여도 204를 반환합니다.
204는 계정 존재나 특정 UUID의 유효 여부를 알려주지 않습니다. 유효한 쿠키를 식별할 수 없는 요청으로
클라이언트가 잃어버린 토큰이나 다른 로그인을 찾아 폐기하지는 않습니다.
동일 로그아웃을 반복해도 갱신 권한을 되살리지 않으며 새 토큰을 발급하지 않습니다.
실행 중 MySQL·SMTP·JWT 서명은 사용하지 않으므로 탈퇴·삭제 회원도 남은 Refresh를 종료할 수 있습니다.
앱 자체의 시작에는 기존 MySQL 연결·스키마 조건이 적용됩니다.

## 동시 갱신·응답 유실·장애

갱신과 로그아웃은 같은 UUID의 Redis hash를 같은 Lua로 처리합니다.

- 로그아웃이 먼저 처리되면 갱신의 inspect 또는 rotate가 revoked를 확인해 거절합니다.
- 갱신의 rotate가 먼저 처리되면 로그아웃은 이전 토큰의 사용 이력을 확인해 새 Refresh까지 후속 갱신을 차단합니다.
- 갱신 성공 응답이 로그아웃 응답보다 늦게 도착해 쿠키가 다시 쓰여도 그 쿠키의 Redis 상태는 폐기되어 갱신할 수 없습니다.

프론트는 로그아웃 시작 시 메모리의 Access Token과 회원 정보를 제거하고 자동 갱신을 중단해야 합니다.
진행 중인 갱신 결과를 로그인 상태 복원에 사용하지 않고, 같은 쿠키를 공유하는 탭에도 로그아웃 상태를 전달해야 합니다.
로그인·갱신·로그아웃의 동시 실행을 조정하고, 늦게 도착한 응답으로 로그아웃 상태를 되돌리지 않습니다.

Redis 연결·실행·응답 오류는 **503 AUTHENTICATION_UNAVAILABLE**입니다. 폐기가 이미 실행됐을 수 있으므로
로그아웃 성공으로 표시하지 않습니다. 서버는 이때 Refresh 쿠키를 지우지 않아 같은 쿠키로 폐기를 재시도할 수 있게 합니다.
프론트는 로컬 로그인 상태를 지운 상태로 유지하고 서버 폐기 결과가 불확실함을 구분합니다.
서버는 자동 재시도하지 않습니다. 클라이언트가 같은 쿠키로 반복 호출하는 것은 멱등이며, 이는
이전 값 재사용이 감지될 수 있는 Refresh 교체의 자동 재시도와 다릅니다.
HTTP 성공 응답이 유실돼도 같은 요청을 다시 보내면 이미 폐기된 상태에서 204와 쿠키 만료를 반환합니다.

## 이미 발급한 Access Token

로그아웃은 Refresh 갱신 권한을 폐기하며 이미 서명한 Access Token의 개별 차단 목록은 사용하지 않습니다.
복사본이 있다면 해당 토큰의 만료 시각까지 접근할 수 있습니다. 기본 수명은 900초이며 시간 검증 오차는 최대 30초입니다.
클라이언트 메모리에서 Access Token을 지우는 것은 필수지만 서버 측 복사본 무효화를 의미하지 않습니다.
탈퇴·삭제 회원은 기존 DB 상태 확인에 의해 다음 인증부터 거절됩니다. 이미 인증을 마친 진행 중 요청은 소급 취소하지 않습니다.

## 오류

| 상황 | HTTP·code | 처리 |
| --- | --- | --- |
| 현재/이전 Refresh, 쿠키 없음·형식 오류·만료·이미 폐기 | 204 | 식별 가능한 현재 로그인만 폐기, Refresh 쿠키 만료, 본문 없음 |
| 같은 이름의 중복 Refresh 쿠키 | 401 REFRESH_TOKEN_INVALID | 폐기 대상을 임의 선택하지 않음. 발급 경로의 쿠키만 만료 |
| CSRF 쿠키·헤더 누락·변조 | 403 INVALID_CSRF_TOKEN | 저장소 폐기·Refresh 쿠키 삭제 전 거절 |
| Redis 작업 결과 불확실 | 503 AUTHENTICATION_UNAVAILABLE | 성공 응답·쿠키 삭제 없음. 같은 쿠키로 폐기 재시도 가능 |
| 예상하지 못한 상태·프로그래밍 오류 | 500 INTERNAL_SERVER_ERROR | 내부 정보 미공개 |

오류는 기존 application/problem+json과 Cache-Control: no-store를 사용합니다.
다른 경로·호스트의 중복 쿠키는 해당 브라우저에서 정리해야 하며 서버는 자신이 발급한 경로의 쿠키만 만료시킵니다.

## 계정 관리와의 범위 구분

#30은 로그인·갱신·로그아웃과 **이미 탈퇴한 회원의 접근 차단**에 더해,
계정 관리용 이메일 본인 확인·비밀번호 변경·회원 탈퇴(계정 삭제)를 포함합니다.
이메일 본인 확인과 [비밀번호 변경·모든 기존 로그인 무효화](password-change.md),
[회원 탈퇴·계정 정리](account-deletion.md)는 각각 별도 API로 구현합니다. 현재 로그아웃 API가 이 작업을 대신 수행하지 않습니다.
User.deletedAt과 UserCredential.passwordChangedAt 필드가 있다는 사실만으로 해당 기능이 구현된 것은 아닙니다.
비밀번호 변경은 DB 인증 버전 증가로 기존 Access/Refresh를 거절하며, 일반 로그아웃은 버전을 증가시키지 않습니다.
회원 탈퇴는 프로필·로그인 수단을 정리하고 공유 기록의 회원 ID를 보존하며 재가입 시 새 회원 ID를 사용합니다.
#31의 탈퇴 계정 재가입·재연결 기준 역시 탈퇴 실행 API 자체를 포함하지 않습니다.
범위 추가와 실제 구현·검증 완료를 구분하며 전체 검증은 상위 #24에서 추적합니다.

## 코드 구조와 확인 범위

| 위치 | 책임 |
| --- | --- |
| auth/presentation/controller/LogoutController | 선택적 Refresh 쿠키·필수 CSRF 명세, 204·쿠키 만료 |
| auth/presentation/cookie/RefreshTokenCookie | 원문 쿠키 추출·중복 거절·동일 속성 쿠키 삭제 |
| auth/application/service/LogoutService | 토큰 형식 확인과 저장소 폐기 포트 호출 |
| auth/application/port/out/RefreshTokenStore | 소유를 확인한 로그인 폐기 계약 |
| auth/infrastructure/redis/RedisRefreshTokenStore·redis/refresh-token-use.lua | 기존 원자적 폐기와 일시적 저장소 장애 변환 |
| global/security/SecurityConfig | Access Token 없이 로그아웃 허용, Bearer 제외, auth CSRF 검사 유지 |

Application은 Redis·JPA·JWT·Servlet·HTTP 또는 Infrastructure/Presentation에 직접 의존하지 않습니다.
이번 단계에서는 Java 21 컴파일·JAR 패키징·앱 시작·OpenAPI 생성·Swagger 표시를 확인합니다.
새 테스트 작성·기존 테스트 변경/실행은 하지 않으며 전체 인증 구현 후 #24에서 검증합니다.
로그아웃 → 갱신 차단, 반복·동시 갱신·다른 로그인 격리·Redis 장애·쿠키 속성·CSRF 정상/누락/변조와
Access Token 잔여 시간의 실제 요청 검증은 미완료입니다.
