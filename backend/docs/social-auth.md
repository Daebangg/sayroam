# OAuth2 소셜 로그인 (#31)

관련 이슈: [#31](https://github.com/Daebangg/sayroam/issues/31), 상위 [#24](https://github.com/Daebangg/sayroam/issues/24).
첫 공급자인 Google에 이어 Kakao·Naver도 같은 인증 흐름을 사용합니다. OAuth2 Authorization Code로 공급자 Access Token을 받은 뒤
UserInfo API에서 사용자를 확인합니다. 서비스의 Access JWT·Refresh Token은 기존 공통 흐름으로 발급합니다.

## 구조

```text
SocialLoginController
  → SocialLoginService / SocialSignupService
    → SocialIdentityProvider (외부 인증 포트)
      → SpringOAuth2IdentityProvider (단일 구현)
        → Spring 코드 교환 + DefaultOAuth2UserService
        → OAuth2UserInfoMapper
    → SocialAccounts (회원·소셜 계정 조회 및 가입)
    → LoginTokenService (공통 토큰 발급)
```

공급자별 프로토콜은 동일하므로 Strategy·Registry·공급자별 서비스는 두지 않습니다.
공급자 차이는 `OAuth2ClientConfig`의 클라이언트 설정과 `OAuth2UserInfoMapper`의 응답 필드 매핑으로 처리합니다.
Application은 공급자 JSON·Servlet·Spring OAuth2 객체를 받지 않고 `SocialIdentity`만 받습니다.
`openid` scope·nonce·ID Token·JWK 검증은 이 흐름에 포함하지 않습니다.

Google·Kakao·Naver의 설정과 사용자 정보 매핑을 같은 공통 흐름으로 구성했습니다.
각 공급자의 활성화 방법은 아래 클라이언트 설정을 따릅니다.
Kakao는 UserInfo 요청에 명세의 form Content-Type을 사용하고 `secure_resource=true`로 HTTPS 프로필 이미지를 요청합니다.
Naver는 토큰 교환에 인가 요청의 state를 포함하고, UserInfo의 `response.id`를 Spring 사용자 식별자로 정규화합니다.
정규화 시 원본 `resultcode`와 `response`를 유지하며, 매퍼에서 성공 코드와 실제 회원 식별자를 검사합니다.
공급자 코드가 구성되어 있어도 각 콘솔의 설정·자격 정보 입력과 실제 로그인 검증은 별도로 필요합니다.
기존 계정 연결·해제는 다음 단계입니다. 같은 이메일이라는 이유로 자동 병합하지 않습니다.

## 클라이언트 설정

### Google

Google Auth Platform에서 Web application 클라이언트를 만들고 **Authorized redirect URIs**에 등록합니다.

```text
http://localhost:8080/api/auth/social/google/callback
```

`backend/config/application-local.properties`의 실제 값은 Git에서 제외합니다.

```properties
app.auth.social.google.enabled=true
app.auth.social.google.client-id=<Client ID>
app.auth.social.google.client-secret=<Client Secret>
app.auth.social.google.redirect-uri=http://localhost:8080/api/auth/social/google/callback
```

### Kakao

1. [Kakao Developers](https://developers.kakao.com/)에서 앱을 선택하고 [카카오 로그인] > [사용 설정]을 ON으로 설정합니다.
2. [앱] > [플랫폼 키] > [REST API 키]에 아래 Redirect URI를 등록합니다. 콜백은 백엔드 주소이며 `code`·`state` 쿼리는 등록하지 않습니다.
3. 같은 REST API 키의 클라이언트 시크릿을 확인하고 개인 설정에 입력합니다. `client-id`는 REST API 키이며 JavaScript 키·어드민 키를 사용하지 않습니다.
4. [카카오 로그인] > [동의항목]에서 필요한 닉네임·프로필 사진·이메일 항목을 설정합니다. 이메일 제공 권한이 없거나 사용자가 동의하지 않아도 기존 이메일 보완 인증으로 가입할 수 있습니다.

```properties
app.auth.social.kakao.enabled=true
app.auth.social.kakao.client-id=<Kakao REST API key>
app.auth.social.kakao.client-secret=<Kakao Login Client Secret>
app.auth.social.kakao.redirect-uri=http://localhost:8080/api/auth/social/kakao/callback
```

설정 파일은 Google과 같은 `backend/config/application-local.properties`입니다. `local` 프로필로 재시작해야 적용됩니다.
실제 키가 준비되지 않았다면 `enabled=false`를 유지합니다. 활성화한 클라이언트의 자격 정보가 비어 있으면 앱 시작 시 설정 검증에서 거절합니다.
이 구현은 OAuth2 Access Token으로 `/v2/user/me`를 조회하며 OpenID Connect의 ID Token을 사용하지 않습니다.
Kakao의 동의항목은 콘솔 설정을 사용하고 `openid` scope나 추가 동의 scope를 강제로 요청하지 않습니다.

### Naver

1. [Naver Developers](https://developers.naver.com/apps/#/register)에서 애플리케이션을 등록하고 사용 API에 [네이버 로그인]을 선택합니다.
2. 필요한 회원 정보 제공 항목으로 이메일·별명·프로필 사진을 설정합니다. 이메일은 기존 이메일 인증으로 보완하고, 닉네임은 가입 요청에서 입력합니다. 프로필 사진은 제공되지 않아도 가입할 수 있습니다.
3. 로그인 오픈 API 서비스 환경에 [PC 웹]을 추가하고 서비스 URL에 `http://localhost:8080`, Callback URL에 아래 주소를 등록합니다.
4. 발급받은 Client ID·Client Secret을 개인 설정 파일에 입력하고 활성화합니다. 개발 상태에서 로그인할 계정이 허용되는지도 콘솔에서 확인합니다.

```properties
app.auth.social.naver.enabled=true
app.auth.social.naver.client-id=<Naver Client ID>
app.auth.social.naver.client-secret=<Naver Client Secret>
app.auth.social.naver.redirect-uri=http://localhost:8080/api/auth/social/naver/callback
```

서비스 URL은 사용자가 방문할 홈 주소이고, Callback URL은 네이버가 인증 결과를 전달할 백엔드 주소입니다.
Kakao·Google의 개인 설정을 유지하면서 Naver 항목을 추가하고 `local` 프로필로 재시작합니다. 키가 준비되지 않았다면 `enabled=false`를 유지합니다.

Naver는 `CLIENT_SECRET_POST`로 Client ID·Secret을 토큰 요청 본문에 전달하고, 같은 요청에 원래 인가 요청의 state도 포함합니다.
사용자 정보는 `GET https://openapi.naver.com/v1/nid/me`에 Bearer 토큰을 넣어 조회합니다.
응답의 `resultcode="00"`을 확인한 뒤 `response.id`를 계정 식별자로 사용하며, 중첩 응답·식별자가 없으면 인증을 거절합니다.
Naver 명세에 따라 scope는 생략하고, 이 인가 코드 흐름에서는 PKCE 파라미터도 전송하지 않습니다. state·공급자·브라우저 바인딩·만료와 일회성 소비 검증은 동일하게 적용합니다.

Naver 프로필에는 이메일 인증 완료 여부가 없으므로, 이 서비스는 신규 가입 시 `emailVerificationRequired=true`를 반환합니다.
제공된 이메일은 입력 참고값이며 기존 #28 이메일 인증으로 `signupProof`를 발급받아 가입합니다. 기존 Naver 회원의 재로그인에는 가입용 이메일 인증을 다시 요구하지 않습니다.
같은 이메일이라는 이유로 Google·Kakao·이메일 계정과 자동 병합하지 않습니다.

### 공통 설정

환경변수는 `APP_AUTH_SOCIAL_GOOGLE_ENABLED`, `APP_AUTH_SOCIAL_GOOGLE_CLIENTID`,
`APP_AUTH_SOCIAL_GOOGLE_CLIENTSECRET`, `APP_AUTH_SOCIAL_GOOGLE_REDIRECTURI`입니다.
Kakao·Naver는 같은 키 형식에서 `google`만 `kakao`·`naver`로 바꿉니다. 기본값은 모두 비활성화입니다.
활성화한 클라이언트는 자격 정보와 `/api/auth/social/{provider}/callback`의 절대 HTTPS URI가 필요합니다.
HTTP는 loopback에서만 허용합니다. 콘솔과 앱의 프로토콜·호스트·포트·경로가 정확히 같아야 합니다.

| 공급자 | 사용자 정보 식별자 | 이메일 보완 기준 | PKCE |
| --- | --- | --- | --- |
| Google | `id` | `verified_email=true`이고 Gmail 또는 `hd`가 있는 계정만 생략 | S256 |
| Kakao | `id` | `kakao_account.is_email_valid`와 `is_email_verified` 모두 true일 때 생략 | S256 |
| Naver | `response.id` | 검증 여부를 제공하지 않으므로 서비스 이메일 인증 필요 | 현재 설정하지 않음 |

Google은 `userinfo.email`, `userinfo.profile` 범위를 요청합니다. Kakao·Naver의 제공 정보는 각 콘솔의 동의 항목을 따릅니다.
공급자 Access Token은 UserInfo 요청에만 사용하고 저장하거나 브라우저에 전달하지 않습니다.
공급자 Refresh Token은 서비스의 로그인 유지에 사용하지 않습니다.

### Kakao 연동 확인 순서

실제 인증은 같은 브라우저와 같은 호스트에서 진행합니다. `localhost`에서 시작했다면 콜백도 `localhost`를 사용해야 바인딩 쿠키가 전달됩니다.
별도 curl에서 시작한 인증 주소만 브라우저로 복사하면 브라우저 바인딩 쿠키가 없어 콜백이 거절됩니다.

1. 같은 브라우저의 Swagger 또는 연결한 프론트에서 CSRF 쿠키·헤더를 준비하고 `POST /api/auth/social/kakao/authorizations`에 `{"returnPath":"/"}`를 전달합니다.
2. 응답의 `authorizationUrl`로 브라우저를 이동합니다. Kakao 클라이언트 ID, 콜백 주소, state와 PKCE S256 challenge가 포함됩니다.
3. 카카오 로그인·정보 제공 동의 후 `/api/auth/social/kakao/callback?code=...&state=...`로 돌아오면 서버가 토큰 교환과 사용자 정보 조회를 수행합니다.
4. 서버가 이동시킨 `/#social_result=...`의 코드를 같은 브라우저에서 `POST /api/auth/social/results/exchange`로 교환합니다. 프론트 연동 전에는 코드를 수동으로 읽어 Swagger에서 교환할 수 있습니다.
5. 신규 계정은 `SIGNUP_REQUIRED`의 티켓으로 닉네임을 제출합니다. `emailVerificationRequired=true`라면 이메일 인증 후 `email`·`signupProof`도 포함합니다.
6. 가입 완료 후 다시 Kakao 인증을 시작하면 같은 `(KAKAO, providerUserId)`로 회원을 찾아 `AUTHENTICATED`와 Sayroam 토큰을 반환합니다.

Kakao 로그인 미활성화는 `KOE004`, Redirect URI 불일치는 `KOE006`을 확인합니다. 토큰 교환 실패는 REST API 키·클라이언트 시크릿과 콜백 URI를 확인합니다.
실제 로그인·동의 취소·이메일 미제공·가입·재로그인 확인을 마쳐야 외부 공급자 연동 검증 완료로 기록합니다.

### Naver 연동 확인 순서

Kakao와 마찬가지로 인증 시작·콜백·결과 교환은 같은 브라우저·호스트에서 진행합니다.

1. CSRF 쿠키·헤더를 준비하고 `POST /api/auth/social/naver/authorizations`에 `{"returnPath":"/"}`를 전달합니다.
2. 응답의 `authorizationUrl`이 `https://nid.naver.com/oauth2.0/authorize`를 가리키며, Client ID·등록한 콜백 주소·state·`response_type=code`가 포함되는지 확인합니다.
3. 같은 브라우저에서 인증 주소를 열어 네이버 로그인·정보 제공 동의를 완료합니다. 브라우저는 `/api/auth/social/naver/callback?code=...&state=...`로 돌아옵니다.
4. 서버가 `/#social_result=...`로 이동시키면 결과 코드를 `POST /api/auth/social/results/exchange`에서 교환합니다.
5. 신규 계정의 `SIGNUP_REQUIRED` 응답에서 이메일 보완 여부를 확인하고, 이메일 인증 후 닉네임·가입 티켓·email·signupProof로 가입합니다.
6. 다시 Naver 인증을 시작하여 같은 `response.id`로 기존 회원을 찾아 `AUTHENTICATED`와 Sayroam 토큰을 반환하는지 확인합니다.

검수 전 계정 제한·서비스 URL 또는 Callback URL 불일치는 Naver 콘솔 설정을 확인합니다.
사용자 취소·state 불일치·만료·이메일 미제공·이메일 충돌도 확인해야 하며, 키가 없는 환경의 컴파일 성공을 실제 Naver 로그인 성공으로 기록하지 않습니다.

2026-11-06 수동 연동에서 실제 Naver 정보 제공 동의 → 콜백 복귀 → 인가 코드 교환 → 사용자 정보 조회 → 결과 교환을 확인했습니다.
결과 교환은 HTTP 200, `status=SIGNUP_REQUIRED`, 가입 티켓 발급, `emailVerificationRequired=true`를 반환했습니다.
Sayroam 가입 완료·이메일 인증·가입 후 재로그인과 취소·실패 조합은 아직 검증하지 않았습니다.
수동 교환도 결과 코드의 기본 유효기간 60초 안에 진행해야 하며, 만료·소비된 결과는 다시 교환하지 않고 새 소셜 인증을 시작합니다.

## API와 브라우저 흐름

모든 POST는 먼저 `GET /api/auth/csrf`로 받은 쿠키와 `X-XSRF-TOKEN` 헤더가 필요합니다.
Authorization 헤더는 사용하지 않습니다. 콜백은 공급자의 GET 이동으로 호출합니다.

### 인증 시작

`POST /api/auth/social/google/authorizations`

```json
{"returnPath":"/"}
```

200 응답의 `authorizationUrl`로 같은 브라우저에서 이동합니다. `expiresInSeconds`의 기본값은 300초입니다.
서버는 state·공급자·목적·브라우저 바인딩 해시·복귀 경로·PKCE verifier를 Redis에 저장합니다.
`returnPath`는 기본 허용 목록 `/`에 정확히 일치해야 합니다. 절대 주소·쿼리·fragment는 허용하지 않습니다.
추가 복귀 경로는 `app.auth.social.allowed-return-paths=/,/trips`처럼 명시합니다.

별도 바인딩 쿠키는 `HttpOnly`, `SameSite=Lax`, `Path=/`입니다.
운영은 `Secure`와 `__Host-sayroam_oauth`, HTTP local은 `sayroam_oauth` 이름을 사용합니다.
현재 로그인 Refresh 쿠키의 `SameSite=Strict`와 별개이며 세션 로그인은 만들지 않습니다.
동시 진행 중인 인증을 위해 유효한 기존 바인딩을 재사용합니다. 서로 다른 경로의 중복 쿠키는 거절합니다.

### 공급자 콜백

`GET /api/auth/social/{provider}/callback?state=...&code=...`

state·공급자·브라우저 바인딩과 만료를 확인하고 Redis에서 일회성 소비합니다.
잘못된 바인딩은 다른 정상 상태를 삭제하지 않습니다. 검증 실패 시 ProblemDetail을 반환하며 리다이렉트하지 않습니다.
검증한 요청은 공급자 코드 교환 → UserInfo 조회 → 소셜 계정 조회 순서로 처리합니다.
기존 회원·가입 대기·취소·실패 결과를 저장한 후 아래처럼 302 이동합니다.

```text
/#social_result=<43자 일회성 결과 코드>
```

URL에는 Access·Refresh Token이나 공급자 오류 원문을 넣지 않습니다. 결과 코드의 기본 TTL은 60초입니다.
콜백 이후 응답에는 `Cache-Control: no-store`, `Referrer-Policy: no-referrer`를 사용합니다.
OpenAPI에는 브라우저 이동용 `x-browser-navigation=true`로 표시합니다.
타입 명세에는 유지하지만 일반 JSON 요청용 `apiRoutes`·`api.call()`에서는 제외합니다.

### 결과 교환

`POST /api/auth/social/results/exchange`

```json
{"resultCode":"<fragment에서 받은 결과 코드>"}
```

화면은 코드를 읽은 뒤 주소에서 fragment를 제거하고 같은 브라우저에서 교환합니다.
바인딩 확인과 결과 소비는 원자 처리합니다. 기존 회원은 현재 소셜 연결과 인증 버전을 다시 조회한 뒤 토큰을 발급합니다.

```json
{"status":"AUTHENTICATED","login":{"accessToken":"...","tokenType":"Bearer","expiresIn":900},"signup":null}
```

Refresh Token은 기존 HttpOnly 쿠키로만 발급합니다. 신규 회원은 일반 서비스 토큰 없이 가입 티켓을 받습니다.

```json
{"status":"SIGNUP_REQUIRED","login":null,"signup":{"signupTicket":"...","expiresInSeconds":600,"emailVerificationRequired":false,"suggestedEmail":"user@gmail.com"}}
```

취소·실패 결과는 교환 시 해당 오류의 ProblemDetail로 반환합니다.

### 가입 완료와 이메일 보완

`POST /api/auth/social/signup`

```json
{"signupTicket":"<가입 티켓>","nickname":"여행자"}
```

`emailVerificationRequired=true`이면 #28의 이메일 코드 발급·확인 API에서 증명을 받은 뒤
`email`, `signupProof`를 추가합니다. 참고용 공급자 이메일과 다른 이메일도 본인 인증을 거치면 사용할 수 있습니다.
신뢰한 공급자 이메일이 있으면 `email`을 생략하거나 같은 값만 전달합니다.

가입 티켓은 브라우저에 묶어 기본 600초 보관하고 한 번만 소비합니다.
`User + UserSocialAccount`를 같은 DB 트랜잭션에서 생성하며 비밀번호 자격 정보는 만들지 않습니다.
닉네임은 앞뒤 공백 제거 후 1~50자입니다. 가입 커밋 후 공통 토큰 발급과 Refresh 쿠키 설정을 수행합니다.
200 응답은 기존 `LoginResponse`와 같습니다.

이메일 충돌은 `EMAIL_ALREADY_REGISTERED`, 소셜 계정 중복은 `SOCIAL_ACCOUNT_CONFLICT`로 거절합니다.
기존 계정으로 로그인한 뒤 명시적으로 연결해야 하며, 연결 API는 후속 단계에서 구현합니다.
탈퇴 회원의 연결은 탈퇴 처리에서 삭제되므로 이후 새 소셜 인증·가입을 거쳐 새 회원으로 생성합니다.

## 저장·실패 처리

Redis의 `sayroam:auth:oauth:transaction`, `result`, `signup` namespace를 분리합니다.
키에는 난수 원문의 SHA-256만 사용하며 바인딩 해시와 payload를 짧게 보관합니다.
NX 저장·TTL 설정, 바인딩 검사·삭제를 각각 Lua로 원자 처리합니다.
기본 TTL 설정은 `transaction-ttl-seconds=300`, `result-ttl-seconds=60`, `signup-ttl-seconds=600`입니다.
브라우저 쿠키는 기본 3600초이며 Redis 상태·가입 티켓의 수명을 연장하지 않습니다.

| HTTP | 주요 code | 처리 |
| --- | --- | --- |
| 400 | `SOCIAL_PROVIDER_DISABLED`, `SOCIAL_RETURN_PATH_INVALID` | 공급자 활성 설정·복귀 경로 확인 |
| 400 | `SOCIAL_AUTH_INVALID`, `SOCIAL_AUTH_CANCELLED`, `SOCIAL_SIGNUP_INVALID` | 바인딩·만료·소비·가입 조건 오류. 새 소셜 인증 필요 |
| 401 | `SOCIAL_AUTH_FAILED`, `AUTHENTICATION_REVOKED` | 공급자 인증 실패 또는 콜백 이후 회원/연결/인증 버전 변경 |
| 403 | `INVALID_CSRF_TOKEN` | POST의 CSRF 쿠키·헤더 확인 |
| 409 | `EMAIL_ALREADY_REGISTERED`, `SOCIAL_ACCOUNT_CONFLICT`, `SIGNUP_PROOF_ALREADY_USED` | 자동 병합·상태 복원 없이 기존 계정 로그인 또는 새 인증 |
| 410 | `SIGNUP_PROOF_EXPIRED` | 이메일 보완 증명 만료 |
| 503 | `SOCIAL_AUTH_UNAVAILABLE`, `AUTHENTICATION_UNAVAILABLE`, `EMAIL_VERIFICATION_UNAVAILABLE` | 저장·커밋·응답 결과가 불확실할 수 있음. 자동 재시도하지 않음 |

소비한 state·결과 코드·가입 티켓·이메일 증명은 후속 실패 시 복원하지 않습니다.
가입 DB 커밋 후 토큰 저장이 실패하면 회원은 이미 생성됐을 수 있으므로 새 소셜 로그인으로 상태를 확인합니다.
일반 입력 DTO 검증 실패는 서비스 진입 전이므로 티켓을 소비하지 않지만, 서비스 진입 후 보완 조건 오류는 소비될 수 있습니다.

## 구현과 검증 범위

인증 시작·콜백·결과 교환·가입·이메일 보완과 OpenAPI를 이번 단계에 포함합니다.
프론트 화면 구현, 기존 계정 연결·해제는 후속 범위입니다.
Kakao·Naver 백엔드의 클라이언트 등록·코드 교환·UserInfo 정규화·기존 회원 로그인·신규 가입·이메일 보완을 같은 공통 흐름에 포함합니다.
콘솔 설정과 실제 자격 정보를 통한 각 공급자 인증은 위 확인 순서로 별도 검증합니다.
추가 테스트 작성·실행은 이슈 지침에 따라 전체 인증 기능 구현 후 진행합니다.
컴파일·앱 시작·OpenAPI 생성 성공을 실제 Google 로그인·이메일 수신·동시성 검증 완료로 표시하지 않습니다.

근거: [Spring OAuth2 사용자 정보 처리](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html#oauth2login-advanced-oauth2-user-service),
[Google OAuth2](https://developers.google.com/identity/protocols/oauth2/web-server),
[Google UserInfo 필드](https://www.googleapis.com/discovery/v1/apis/oauth2/v2/rest),
[Kakao REST API](https://developers.kakao.com/docs/ko/kakaologin/rest-api),
[Naver 로그인](https://developers.naver.com/docs/login/api/api.md),
[Naver 프로필](https://developers.naver.com/docs/login/profile/profile.md).
