/** OpenAPI에서 자동 생성합니다. 직접 수정하지 말고 pnpm api:generate를 실행하세요. */
export interface paths {
    "/api/auth/account-deletion": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 본인 회원 탈퇴·계정 프로필 및 로그인 수단 정리
         * @description Bearer 인증과 CSRF 쿠키·X-XSRF-TOKEN 헤더 필요. ACCOUNT_DELETION 이메일 본인 확인의 accountProof만 한 번 소비합니다. 현재 회원의 탈퇴 상태·프로필 익명화·인증 버전 증가와 비밀번호·소셜 로그인 연결·개인 여행 성향·수신 알림 삭제를 한 DB 트랜잭션으로 커밋합니다. 회원 ID와 채팅·정산 등 공유 기록의 참조는 보존합니다. 모든 기기의 기존 로그인·Access 인증·Refresh 갱신을 차단하고 현재 Refresh 쿠키를 만료시킵니다. 같은 이메일로 재가입하면 새 회원이며 기존 계정을 복원하지 않습니다. 응답 유실·DB 커밋 결과 불확실 시 자동 재전송·자동 재가입하지 마세요.
         */
        post: operations["delete"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/auth/account-verifications": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 계정 관리용 이메일 본인 확인 코드 발급·재발송
         * @description Bearer 인증과 CSRF 쿠키·X-XSRF-TOKEN 헤더가 모두 필요합니다. DB의 현재 인증된 대표 이메일로만 발송합니다. PASSWORD_CHANGE/ACCOUNT_DELETION의 코드·증명은 분리하고 발송 제한은 합산합니다. 승인된 재발급은 같은 목적의 이전 코드·증명을 무효화합니다. 이 API는 비밀번호 변경·탈퇴를 실행하지 않습니다.
         */
        post: operations["request_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/auth/account-verifications/verify": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 계정 관리용 코드 검증·일회성 증명 발급
         * @description 같은 회원·현재 이메일·목적·인증 버전·요청 ID·코드를 확인한 후 코드를 소비하고 accountProof를 한 번 반환합니다. 증명은 해당 목적의 작업에서 한 번만 소비할 수 있습니다. 가입용 signupProof와 호환되지 않습니다. 확인 응답을 잃으면 코드를 재사용하지 말고 새로 발급해야 합니다. PASSWORD_CHANGE 증명은 PATCH /api/auth/password, ACCOUNT_DELETION 증명은 POST /api/auth/account-deletion에서 사용합니다.
         */
        post: operations["confirm_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/auth/csrf": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * CSRF 쿠키 최초 발급
         * @description 응답의 XSRF-TOKEN 쿠키 값을 로그인·가입·토큰 갱신·로그아웃 등 이후 변경 요청의 X-XSRF-TOKEN 헤더에 전달합니다. 토큰은 응답 본문에 넣지 않습니다. 로그인·갱신·로그아웃은 현재 CSRF 쿠키를 유지하며 쿠키가 없거나 만료됐으면 다시 준비합니다.
         */
        get: operations["csrf"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/auth/email-verifications": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 이메일 인증 코드 발급·재발송
         * @description CSRF 쿠키와 X-XSRF-TOKEN 헤더 필요. 코드는 6자리이며 응답에 포함하지 않습니다. 재발급 승인 시 이전 코드와 가입용 증명은 무효화합니다.
         */
        post: operations["request"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/auth/email-verifications/verify": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 이메일 인증 코드 검증 및 가입용 증명 발급
         * @description 이메일·요청 ID·코드를 원자적으로 확인하고 코드를 소비합니다. 성공한 코드는 다시 사용할 수 없습니다. SMTP 설정 없이 기존 코드 검증은 가능하지만 Redis는 필요합니다. 가입용 증명은 POST /api/auth/signup에서 한 번 사용할 수 있습니다.
         */
        post: operations["confirm"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/auth/login": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 이메일·비밀번호 로그인
         * @description XSRF-TOKEN 쿠키와 X-XSRF-TOKEN 헤더 필요. 활성·이메일 인증 회원의 비밀번호를 확인해 Access Token을 본문에, Refresh Token을 HttpOnly 쿠키에 발급합니다. Redis 저장이 완료된 뒤 응답합니다. 이메일 없음·비밀번호 불일치·탈퇴·비밀번호 미등록은 같은 실패 응답입니다. Authorization 헤더 없이 호출하세요.
         */
        post: operations["login"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/auth/logout": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 현재 로그인 로그아웃
         * @description XSRF-TOKEN 쿠키·X-XSRF-TOKEN 헤더가 필요합니다. sayroam_refresh 쿠키에 연결된 로그인만 갱신 불가로 만들고 쿠키를 만료시킵니다. Access Token은 필요하지 않으며 Authorization 헤더는 무시합니다. 쿠키 없음·형식 오류·만료·이미 폐기된 토큰도 204로 정리합니다. 같은 이름의 중복 쿠키는 401로 거절합니다. 이미 발급한 Access Token은 만료(기본 15분, 최대 30초 시간 오차)까지 유효하므로 클라이언트 메모리에서도 제거해야 합니다. 모든 기기 로그아웃·계정 삭제·비밀번호 변경은 포함하지 않습니다.
         */
        post: operations["logout"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/auth/password": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        /**
         * 본인 비밀번호 변경·모든 기존 로그인 무효화
         * @description Bearer 인증과 CSRF 쿠키·X-XSRF-TOKEN 헤더 필요. PASSWORD_CHANGE 이메일 본인 확인의 accountProof를 한 번 소비합니다. 기존 비밀번호가 등록된 본인 계정만 변경하며 소셜 전용 계정에 비밀번호를 새로 등록하지 않습니다. 해시·변경 시각·인증 버전 증가를 같은 DB 트랜잭션으로 커밋합니다. 모든 기기의 기존 Access/Refresh는 이후 인증·갱신에서 거절됩니다. 성공 시 현재 Refresh 쿠키를 만료시키며 프론트는 Access Token·회원 정보·진행 중 갱신을 정리하고 새 비밀번호로 재로그인해야 합니다. 응답 유실 시 자동 재전송하지 마세요.
         */
        patch: operations["change"];
        trace?: never;
    };
    "/api/auth/refresh": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 토큰 갱신
         * @description 브라우저의 sayroam_refresh HttpOnly 쿠키와 XSRF-TOKEN 쿠키·X-XSRF-TOKEN 헤더가 필요합니다. Access Token 없이 호출하며 이 경로에서는 Authorization 헤더를 인증에 사용하지 않습니다. 활성 회원을 확인한 뒤 Refresh Token을 일회성 교체합니다. 이전 값 재사용·동시 갱신은 해당 로그인의 갱신 권한을 폐기합니다. 클라이언트는 갱신 요청을 직렬화하고 불확실한 응답을 자동 재시도하지 않아야 합니다. 절대 만료는 연장하지 않습니다.
         */
        post: operations["refresh"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/auth/signup": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 이메일 인증 후 회원가입
         * @description CSRF 쿠키와 X-XSRF-TOKEN 헤더 필요. 가입용 증명을 먼저 일회성 소비한 뒤 일반 회원과 비밀번호 해시를 함께 저장합니다. DB 실패 시 증명을 복구하지 않으며 새 이메일 인증이 필요합니다. 자동 로그인과 JWT 발급은 수행하지 않습니다.
         */
        post: operations["signup"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/auth/social/{provider}/authorizations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 소셜 로그인 인증 시작
         * @description 활성화한 OAuth2 공급자만 사용합니다. 첫 공급자는 google. CSRF 쿠키·헤더 필요. HttpOnly 브라우저 바인딩 쿠키를 설정하고 state 및 지원 공급자의 PKCE S256 인증 주소를 반환합니다. 같은 브라우저에서 authorizationUrl로 이동하세요. returnPath는 서버 허용 목록만 사용합니다. Bearer 헤더 없이 호출하세요.
         */
        post: operations["startSocialLogin"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/auth/social/{provider}/callback": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 공급자 인증 콜백
         * @description 공급자가 GET 리다이렉트로 호출합니다. state·공급자·브라우저 바인딩을 검사해 일회성 소비합니다. 검증한 콜백만 허용된 returnPath#social_result=<일회성 코드>로 302 이동합니다. 바인딩 오류는 리다이렉트하지 않고 ProblemDetail을 반환합니다. 기존 Refresh 쿠키·Bearer 헤더·세션에 의존하지 않습니다.
         */
        get: operations["socialLoginCallback"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/auth/social/results/exchange": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 소셜 로그인 결과 일회성 교환
         * @description 인증을 시작한 브라우저의 HttpOnly 바인딩 쿠키와 CSRF 쿠키·헤더 필요. social_result 값을 읽은 뒤 주소에서 제거하세요. AUTHENTICATED이면 login 본문과 Refresh 쿠키, SIGNUP_REQUIRED이면 signup 티켓·이메일 보완 여부를 반환합니다. 실패 결과는 해당 ProblemDetail로 반환합니다. 소비 후 응답 유실·장애 시 자동 재시도하지 말고 소셜 인증을 다시 시작하세요.
         */
        post: operations["exchangeSocialLoginResult"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/auth/social/signup": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 소셜 회원가입 완료·로그인
         * @description 동일 브라우저 바인딩과 CSRF 쿠키·헤더 필요. 가입 티켓·닉네임을 전달합니다. 이메일 보완이 필요하면 #28의 코드 발급·확인 후 email/signupProof도 전달합니다. 비밀번호 자격 정보 없이 User+소셜 계정을 한 DB 트랜잭션에서 생성합니다. 이메일 충돌 시 자동 병합하지 않으며 기존 계정 로그인 후 연결해야 합니다. 티켓·증명 소비 또는 DB 커밋 이후 실패 시 복원·자동 재시도하지 않습니다.
         */
        post: operations["completeSocialSignup"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/rooms": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 채팅방 생성
         * @description 로그인한 회원을 방장으로 하여 방을 생성합니다.
         */
        post: operations["create"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/users/me": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 내 회원 정보 조회
         * @description Authorization: Bearer <accessToken> 필요. 토큰 검증 후 DB에서 조회한 현재 회원 정보를 반환합니다. 탈퇴·삭제 회원은 인증되지 않으며 요청에서 다른 회원의 ID를 받지 않습니다.
         */
        get: operations["me"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        AccountDeletionRequest: {
            /** @description ACCOUNT_DELETION 본인 확인에서 발급한 일회성 증명 */
            accountProof: string;
        };
        AccountVerificationConfirmRequest: {
            /**
             * @description 메일로 받은 6자리 숫자 문자열
             * @example 012345
             */
            code: string;
            /**
             * @description 발급 시와 같은 목적
             * @example PASSWORD_CHANGE
             * @enum {string}
             */
            purpose: "PASSWORD_CHANGE" | "ACCOUNT_DELETION";
            /**
             * Format: uuid
             * @description 발급 API가 반환한 verificationId
             */
            verificationId: string;
        };
        AccountVerificationConfirmResponse: {
            /** @description 회원·현재 이메일·목적에 묶인 일회성 난수 증명. 가입용 증명·로그인 토큰이 아님 */
            accountProof: string;
            /**
             * Format: int64
             * @description 증명 만료까지 남은 초
             */
            expiresInSeconds: number;
            /** @enum {string} */
            purpose: "PASSWORD_CHANGE" | "ACCOUNT_DELETION";
        };
        AccountVerificationRequest: {
            /**
             * @description 본인 확인 목적. 회원·이메일은 인증된 계정에서 결정
             * @example PASSWORD_CHANGE
             * @enum {string}
             */
            purpose: "PASSWORD_CHANGE" | "ACCOUNT_DELETION";
        };
        AccountVerificationResponse: {
            /**
             * Format: int64
             * @description 코드 만료까지 남은 초
             */
            expiresInSeconds: number;
            /** @enum {string} */
            purpose: "PASSWORD_CHANGE" | "ACCOUNT_DELETION";
            /**
             * Format: int64
             * @description 같은 회원이 어느 목적이든 다시 발송하기 전 대기 초
             */
            resendAfterSeconds: number;
            /** @description 확인 요청을 식별하는 UUID */
            verificationId: string;
        };
        /**
         * @description 백엔드에 정의된 업무·공통 오류 코드. Spring 기본 요청 오류와 향후 추가 코드는 별도 문자열로 받을 수 있습니다.
         * @enum {string}
         */
        ApiErrorCode:
            | "ACCESS_DENIED"
            | "ACCOUNT_DELETION_CONFLICT"
            | "ACCOUNT_DELETION_UNAVAILABLE"
            | "ACCOUNT_PROOF_ALREADY_USED"
            | "ACCOUNT_PROOF_EXPIRED"
            | "ACCOUNT_PROOF_INVALID"
            | "ACCOUNT_VERIFICATION_NOT_ALLOWED"
            | "AUTHENTICATION_REVOKED"
            | "AUTHENTICATION_UNAVAILABLE"
            | "EMAIL_ALREADY_REGISTERED"
            | "EMAIL_CODE_ALREADY_USED"
            | "EMAIL_CODE_ATTEMPTS_EXCEEDED"
            | "EMAIL_CODE_EXPIRED"
            | "EMAIL_CODE_INVALID"
            | "EMAIL_DELIVERY_DISABLED"
            | "EMAIL_DELIVERY_FAILED"
            | "EMAIL_RATE_LIMITED"
            | "EMAIL_REQUEST_SUPERSEDED"
            | "EMAIL_VERIFICATION_UNAVAILABLE"
            | "INTERNAL_SERVER_ERROR"
            | "INVALID_CREDENTIALS"
            | "INVALID_CSRF_TOKEN"
            | "INVALID_INPUT"
            | "PASSWORD_CHANGE_CONFLICT"
            | "PASSWORD_CHANGE_NOT_ALLOWED"
            | "PASSWORD_CHANGE_UNAVAILABLE"
            | "PASSWORD_POLICY_VIOLATION"
            | "REFRESH_TOKEN_INVALID"
            | "REFRESH_TOKEN_REUSED"
            | "ROOM_NAME_TOO_LONG"
            | "SIGNUP_PROOF_ALREADY_USED"
            | "SIGNUP_PROOF_EXPIRED"
            | "SIGNUP_PROOF_INVALID"
            | "SIGNUP_UNAVAILABLE"
            | "SOCIAL_ACCOUNT_CONFLICT"
            | "SOCIAL_AUTH_CANCELLED"
            | "SOCIAL_AUTH_FAILED"
            | "SOCIAL_AUTH_INVALID"
            | "SOCIAL_AUTH_UNAVAILABLE"
            | "SOCIAL_PROVIDER_DISABLED"
            | "SOCIAL_RETURN_PATH_INVALID"
            | "SOCIAL_SIGNUP_INVALID"
            | "UNAUTHENTICATED";
        ApiProblem: {
            /** @example UNAUTHENTICATED */
            code: components["schemas"]["ApiErrorCode"] | string;
            /** @example Authentication is required */
            detail: string;
            errors?: components["schemas"]["FieldError"][];
            /**
             * Format: uri-reference
             * @example /api/users/me
             */
            instance: string;
            /**
             * Format: int64
             * @description 재시도 대기가 있는 429 응답에만 포함하는 대기 초. Retry-After 헤더와 같은 값
             */
            retryAfterSeconds?: number;
            /**
             * Format: int32
             * @example 401
             */
            status: number;
            /** @example Unauthorized */
            title: string;
            /**
             * Format: uri
             * @description 생략하면 about:blank로 해석합니다.
             */
            type?: string;
        };
        CreateRoomRequest: {
            /**
             * @description 방 이름. 선택값이며 앞뒤 공백 제거 후 20자 이내. 공백뿐이면 이름 없는 방으로 저장
             * @example 제주 3박 4일
             */
            name?: string | null;
        };
        CreateRoomResponse: {
            /**
             * Format: int64
             * @description 생성된 방 ID
             * @example 7
             */
            roomId: number;
        };
        CurrentUserResponse: {
            /** @example user@example.com */
            email: string | null;
            /** @example 여행자 */
            nickname: string;
            profileImageUrl: string | null;
            /** @enum {string} */
            role: "USER" | "ADMIN";
            /**
             * Format: int64
             * @example 1
             */
            userId: number;
        };
        EmailRequest: {
            /**
             * Format: email
             * @description 앞뒤 공백 제거 후 ASCII 이메일을 소문자로 정규화. 가입·로그인에도 같은 기준 적용 예정
             * @example user@example.com
             */
            email: string;
        };
        EmailVerificationConfirmRequest: {
            /**
             * @description 메일로 받은 6자리 숫자. 앞자리 0을 보존하는 문자열
             * @example 012345
             */
            code: string;
            /**
             * Format: email
             * @description 발급 시와 같은 이메일. 공백 제거 후 소문자로 정규화
             * @example user@example.com
             */
            email: string;
            /**
             * Format: uuid
             * @description 발급 API가 반환한 verificationId
             */
            verificationId: string;
        };
        EmailVerificationConfirmResponse: {
            /**
             * Format: int64
             * @description 가입용 증명이 만료될 때까지 남은 초
             */
            expiresInSeconds: number;
            /** @description 이 이메일의 가입에만 한 번 사용할 난수 증명. JWT·로그인 토큰이 아님 */
            signupProof: string;
        };
        FieldError: {
            field: string;
            reason: string;
        };
        IssuedRequest: {
            /** Format: int64 */
            expiresInSeconds: number;
            /** Format: int64 */
            resendAfterSeconds: number;
            verificationId: string;
        };
        LoginRequest: {
            /**
             * Format: email
             * @description 가입한 이메일. 앞뒤 공백 제거 후 소문자로 정규화
             * @example user@example.com
             */
            email: string;
            /** @description 가입한 비밀번호. 공백·대소문자를 그대로 검증 */
            password: string;
        };
        LoginResponse: {
            /** @description API 요청의 Authorization 헤더로 전달하는 Access Token */
            accessToken: string;
            /**
             * Format: int64
             * @description 토큰 유효기간(초)
             * @example 900
             */
            expiresIn: number;
            /**
             * @description 인증 방식
             * @enum {string}
             */
            tokenType: "Bearer";
        };
        PasswordChangeRequest: {
            /** @description PASSWORD_CHANGE 본인 확인에서 발급한 일회성 증명 */
            accountProof: string;
            /** @description 가입과 같은 8~128자 정책. 공백 제거·대소문자 변환 없이 해시 처리 */
            newPassword: string;
        };
        Signup: {
            /** @description true이면 #28 이메일 인증 후 signupProof 필요 */
            emailVerificationRequired: boolean;
            /**
             * Format: int64
             * @description 가입 티켓 유효기간(초)
             */
            expiresInSeconds: number;
            /** @description 가입 API에 한 번 전달할 브라우저 바인딩 티켓 */
            signupTicket: string;
            /** @description 공급자가 제공한 입력 참고용 이메일. 검증 완료를 의미하지 않음 */
            suggestedEmail?: string | null;
        };
        SignupRequest: {
            /**
             * Format: email
             * @description 인증한 이메일. 공백 제거 후 소문자로 정규화
             * @example user@example.com
             */
            email: string;
            /**
             * @description 앞뒤 공백 제거 후 1~50자. 제어 문자 불가
             * @example 여행자
             */
            nickname: string;
            /** @description 8~128자. 공백 제거·대소문자 변환 없이 해시 처리 */
            password: string;
            /** @description 코드 검증 API에서 실제로 받은 43자 가입용 증명 */
            signupProof: string;
        };
        SignupResponse: {
            /**
             * Format: int64
             * @description 생성된 회원 ID. 가입은 자동 로그인을 수행하지 않음
             */
            userId: number;
        };
        SocialAuthStartRequest: {
            /**
             * @description 서버 허용 목록의 로컬 복귀 경로. 생략하면 /
             * @default /
             */
            returnPath: string;
        };
        SocialAuthStartResponse: {
            /**
             * Format: uri
             * @description 응답을 받은 같은 브라우저에서 이동할 공급자 인증 주소
             */
            authorizationUrl: string;
            /**
             * Format: int64
             * @description 인증 요청 유효기간(초)
             */
            expiresInSeconds: number;
        };
        SocialExchangeResponse: {
            /** @description AUTHENTICATED일 때만 존재. Refresh는 HttpOnly 쿠키 */
            login?: components["schemas"]["LoginResponse"] | null;
            /** @description SIGNUP_REQUIRED일 때만 존재. 가입 전 일반 서비스 토큰은 발급하지 않음 */
            signup?: components["schemas"]["Signup"] | null;
            /** @enum {string} */
            status: "AUTHENTICATED" | "SIGNUP_REQUIRED";
        };
        SocialResultExchangeRequest: {
            /** @description 콜백 후 화면 fragment의 social_result 값. 일회성·브라우저 바인딩 */
            resultCode: string;
        };
        SocialSignupRequest: {
            /**
             * Format: email
             * @description 이메일 보완 시 인증한 이메일. 신뢰한 공급자 이메일이 있으면 생략하거나 동일한 값만 전달
             */
            email?: string | null;
            /** @description 앞뒤 공백 제거 후 1~50자 */
            nickname: string;
            /** @description 이메일 보완 시 #28에서 받은 가입 증명 */
            signupProof?: string | null;
            /** @description 결과 교환에서 받은 가입 티켓 */
            signupTicket: string;
        };
    };
    responses: {
        /** @description 401: UNAUTHENTICATED 또는 AUTHENTICATION_REVOKED. 현재 DB 인증 버전과 다른 로그인은 거절. 재로그인 필요. WWW-Authenticate: Bearer */
        AccountAuthenticationRevoked: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 503: AUTHENTICATION_UNAVAILABLE 또는 EMAIL_VERIFICATION_UNAVAILABLE. 코드 소비·증명 발급 결과가 불확실할 수 있어 자동 재시도하지 않고 새 코드 요청. SMTP 없이 기존 코드 확인 가능 */
        AccountConfirmationUnavailable: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 400: INVALID_INPUT, ACCOUNT_PROOF_INVALID 또는 Spring 요청 오류. 현재 회원의 ACCOUNT_DELETION 증명 필요 */
        AccountDeletionBadRequest: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 409: ACCOUNT_PROOF_ALREADY_USED 또는 ACCOUNT_DELETION_CONFLICT. 소비한 증명은 복원하지 않음. 활성 계정이 남아 있을 때만 재로그인 후 새 본인 확인으로 재시도 */
        AccountDeletionConflict: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 503: AUTHENTICATION_UNAVAILABLE(사전 회원 조회), EMAIL_VERIFICATION_UNAVAILABLE(증명 소비 결과 불확실) 또는 ACCOUNT_DELETION_UNAVAILABLE(DB 커밋 결과 불확실). 마지막 코드일 때만 Refresh 쿠키 만료. 로컬 로그인 상태 정리 후 완료 여부 불확실 상태를 표시하며 자동 재전송·자동 재가입하지 않음 */
        AccountDeletionUnavailable: {
            headers: {
                /** @description ACCOUNT_DELETION_UNAVAILABLE일 때만 sayroam_refresh 만료. 증명/사전 조회 단계의 503에서는 설정하지 않음 */
                "Set-Cookie"?: string;
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 410: ACCOUNT_PROOF_EXPIRED. 없거나 만료된 증명 또는 현재 회원·이메일·목적·인증 버전의 증명이 없음. 새 본인 확인 필요 */
        AccountProofExpired: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 403: ACCESS_DENIED, INVALID_CSRF_TOKEN 또는 ACCOUNT_VERIFICATION_NOT_ALLOWED. 활성 계정의 인증된 대표 이메일이 필요 */
        AccountVerificationForbidden: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 503: AUTHENTICATION_UNAVAILABLE, EMAIL_DELIVERY_DISABLED 또는 EMAIL_VERIFICATION_UNAVAILABLE. 회원 DB·SMTP 설정·Redis 문제. 발급 결과가 불확실하면 자동 재시도하지 않고 제한 시간 이후 새 코드 요청 */
        AccountVerificationUnavailable: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 503: AUTHENTICATION_UNAVAILABLE. 회원 DB 또는 Refresh Redis 저장소 장애. 성공 응답 없이 거절하며 갱신 결과가 불확실할 수 있으므로 자동 재시도하지 않음 */
        AuthenticationUnavailable: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 400: INVALID_INPUT 또는 Spring 요청 오류. 검증 실패 시 errors 포함 */
        BadRequest: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 429: EMAIL_CODE_ATTEMPTS_EXCEEDED. 이 코드는 더 사용할 수 없으며 새 코드 발급 필요. Retry-After는 제공하지 않음 */
        EmailCodeAttemptsExceeded: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 410: EMAIL_CODE_EXPIRED. 없거나 만료된 코드. 새 코드 발급 필요 */
        EmailCodeExpired: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 400: INVALID_INPUT, EMAIL_CODE_INVALID 또는 Spring 요청 오류. 형식 오류는 수정, 코드 불일치는 남은 횟수 안에서 재입력 */
        EmailCodeInvalid: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 503: EMAIL_VERIFICATION_UNAVAILABLE. Redis 작업 결과가 불확실할 수 있음. 코드가 소비됐으면 새 코드 발급 필요 */
        EmailConfirmationUnavailable: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 502: EMAIL_DELIVERY_FAILED. 코드 무효화, 발송 제한 유지. 자동 재시도 없음 */
        EmailDeliveryFailed: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 429: EMAIL_RATE_LIMITED. Retry-After 헤더와 retryAfterSeconds 이후 재요청 */
        EmailRateLimited: {
            headers: {
                /** @description 재시도 전 대기할 초 */
                "Retry-After"?: number;
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 409: EMAIL_REQUEST_SUPERSEDED. 만료·교체된 요청이며 최신 요청을 사용해야 함 */
        EmailRequestSuperseded: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 409: EMAIL_REQUEST_SUPERSEDED 또는 EMAIL_CODE_ALREADY_USED. 최신 요청 또는 이미 받은 증명을 사용 */
        EmailVerificationConflict: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 503: EMAIL_DELIVERY_DISABLED 또는 EMAIL_VERIFICATION_UNAVAILABLE. 설정·저장소 복구 후 재요청 */
        EmailVerificationUnavailable: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 403: ACCESS_DENIED 또는 INVALID_CSRF_TOKEN */
        Forbidden: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 500: INTERNAL_SERVER_ERROR */
        InternalServerError: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 401: INVALID_CREDENTIALS. 이메일 없음·비밀번호 불일치·탈퇴·이메일 미인증·비밀번호 미등록에 동일한 응답. 불필요한 Authorization 헤더가 있으면 필터에서 UNAUTHENTICATED로 거절될 수 있음 */
        LoginUnauthorized: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 401: REFRESH_TOKEN_INVALID. 같은 이름의 Refresh 쿠키가 중복되어 폐기 대상을 선택할 수 없음. 발급 경로의 쿠키만 만료시키며 다른 경로·호스트의 중복 쿠키는 브라우저에서 정리해야 함 */
        LogoutCookieInvalid: {
            headers: {
                /** @description sayroam_refresh 만료. 발급과 같은 경로·속성으로 Max-Age=0 */
                "Set-Cookie"?: string;
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 503: AUTHENTICATION_UNAVAILABLE. Redis 폐기 결과를 확인할 수 없음. 성공 응답·Refresh 쿠키 삭제 없이 실패하며 같은 쿠키로 재시도 가능. 폐기는 이미 실행됐을 수도 있음 */
        LogoutUnavailable: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 400: INVALID_INPUT, PASSWORD_POLICY_VIOLATION, ACCOUNT_PROOF_INVALID 또는 Spring 요청 오류. PASSWORD_CHANGE 증명과 8~128자의 새 비밀번호 필요. 비밀번호 정책 실패는 증명을 소비하지 않음 */
        PasswordChangeBadRequest: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 409: ACCOUNT_PROOF_ALREADY_USED 또는 PASSWORD_CHANGE_CONFLICT. 소비된 증명 복원 없음. 회원 상태·이메일·인증 버전 충돌 시 재로그인 후 새 본인 확인 필요 */
        PasswordChangeConflict: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 403: ACCESS_DENIED, INVALID_CSRF_TOKEN, ACCOUNT_VERIFICATION_NOT_ALLOWED 또는 PASSWORD_CHANGE_NOT_ALLOWED. 활성·이메일 인증·기존 비밀번호 등록 계정만 허용. 소셜 전용 계정의 비밀번호 등록은 지원하지 않음 */
        PasswordChangeForbidden: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 503: AUTHENTICATION_UNAVAILABLE(사전 DB 조회), EMAIL_VERIFICATION_UNAVAILABLE(증명 소비 결과 불확실) 또는 PASSWORD_CHANGE_UNAVAILABLE(DB 변경·커밋 결과 불확실). 자동 재시도 금지. 마지막 코드일 때만 Refresh 쿠키를 만료시키며 프론트는 로그인 상태를 지우고 재로그인으로 결과 확인 */
        PasswordChangeUnavailable: {
            headers: {
                /** @description PASSWORD_CHANGE_UNAVAILABLE일 때 sayroam_refresh 만료. 나머지 503에서는 설정하지 않음 */
                "Set-Cookie"?: string;
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 401: REFRESH_TOKEN_INVALID 또는 REFRESH_TOKEN_REUSED. 쿠키 누락·중복·형식 오류·만료·폐기·회원 삭제/탈퇴·교체 한도, 또는 사용한 토큰 재사용. Refresh 쿠키를 만료시키며 재로그인 필요. 재사용은 해당 로그인만 갱신 불가로 만듦 */
        RefreshUnauthorized: {
            headers: {
                /** @description sayroam_refresh=; Path=/api/auth; Max-Age=0; HttpOnly; SameSite=Strict. Secure 설정은 발급과 동일 */
                "Set-Cookie"?: string;
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 400: ROOM_NAME_TOO_LONG 또는 Spring 요청 오류. 이름을 20자 이내로 수정 후 재요청 */
        RoomBadRequest: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 400: INVALID_INPUT, PASSWORD_POLICY_VIOLATION, SIGNUP_PROOF_INVALID 또는 Spring 요청 오류. 입력·비밀번호 정책 검증 실패는 증명을 소비하지 않음 */
        SignupBadRequest: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 409: SIGNUP_PROOF_ALREADY_USED 또는 EMAIL_ALREADY_REGISTERED. 이미 소비한 증명은 복구하지 않음 */
        SignupConflict: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 410: SIGNUP_PROOF_EXPIRED. 없거나 만료된 증명. 새 이메일 인증 필요 */
        SignupProofExpired: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 503: EMAIL_VERIFICATION_UNAVAILABLE 또는 SIGNUP_UNAVAILABLE. 증명 소비·DB 커밋 결과가 불확실할 수 있음. 자동 재시도 없이 새 인증 필요; 계정은 이미 생성됐을 수도 있음 */
        SignupUnavailable: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 400: SOCIAL_PROVIDER_DISABLED, SOCIAL_AUTH_INVALID, SOCIAL_AUTH_CANCELLED, SOCIAL_RETURN_PATH_INVALID, SOCIAL_SIGNUP_INVALID, SIGNUP_PROOF_INVALID 또는 INVALID_INPUT. 만료·소비된 상태나 잘못된 브라우저이면 소셜 인증부터 다시 시작 */
        SocialBadRequest: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 409: EMAIL_ALREADY_REGISTERED, SOCIAL_ACCOUNT_CONFLICT 또는 SIGNUP_PROOF_ALREADY_USED. 이메일 자동 병합 없음. 기존 회원은 기존 계정으로 로그인 후 명시적 연결 필요 */
        SocialConflict: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 401: SOCIAL_AUTH_FAILED 또는 AUTHENTICATION_REVOKED. 공급자 인증 실패·기존 로그인 수단/인증 버전 변경. 새 소셜 인증 필요 */
        SocialUnauthorized: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 503: SOCIAL_AUTH_UNAVAILABLE, AUTHENTICATION_UNAVAILABLE 또는 EMAIL_VERIFICATION_UNAVAILABLE. 상태·증명 소비나 가입 커밋 결과가 불확실할 수 있음. 자동 재시도 없이 소셜 인증부터 다시 시작 */
        SocialUnavailable: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
        /** @description 401: UNAUTHENTICATED. 토큰 누락·만료·변조·잘못된 발급자/대상·유형·인증 버전 또는 탈퇴·삭제 회원. WWW-Authenticate: Bearer */
        Unauthorized: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["ApiProblem"];
            };
        };
    };
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    delete: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["AccountDeletionRequest"];
            };
        };
        responses: {
            /** @description 탈퇴 및 계정 정리 완료. Cache-Control: no-store. 본문·새 토큰 없음 */
            204: {
                headers: {
                    /** @description 발급과 같은 속성으로 sayroam_refresh 만료. XSRF-TOKEN은 유지 */
                    "Set-Cookie"?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
            400: components["responses"]["AccountDeletionBadRequest"];
            401: components["responses"]["AccountAuthenticationRevoked"];
            403: components["responses"]["AccountVerificationForbidden"];
            409: components["responses"]["AccountDeletionConflict"];
            410: components["responses"]["AccountProofExpired"];
            500: components["responses"]["InternalServerError"];
            503: components["responses"]["AccountDeletionUnavailable"];
        };
    };
    request_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["AccountVerificationRequest"];
            };
        };
        responses: {
            /** @description SMTP 서버에 발송 요청 완료. 수신함 도착을 보장하지 않음. Cache-Control: no-store */
            202: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AccountVerificationResponse"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["AccountAuthenticationRevoked"];
            403: components["responses"]["AccountVerificationForbidden"];
            409: components["responses"]["EmailRequestSuperseded"];
            429: components["responses"]["EmailRateLimited"];
            500: components["responses"]["InternalServerError"];
            502: components["responses"]["EmailDeliveryFailed"];
            503: components["responses"]["AccountVerificationUnavailable"];
        };
    };
    confirm_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["AccountVerificationConfirmRequest"];
            };
        };
        responses: {
            /** @description 목적별 일회성 accountProof. Cache-Control: no-store */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AccountVerificationConfirmResponse"];
                };
            };
            400: components["responses"]["EmailCodeInvalid"];
            401: components["responses"]["AccountAuthenticationRevoked"];
            403: components["responses"]["AccountVerificationForbidden"];
            409: components["responses"]["EmailVerificationConflict"];
            410: components["responses"]["EmailCodeExpired"];
            429: components["responses"]["EmailCodeAttemptsExceeded"];
            500: components["responses"]["InternalServerError"];
            503: components["responses"]["AccountConfirmationUnavailable"];
        };
    };
    csrf: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description CSRF 쿠키 준비 완료 (Cache-Control: no-store) */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    request: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EmailRequest"];
            };
        };
        responses: {
            /** @description SMTP 서버에 발송 요청 완료. 수신함 도착을 보장하지 않음 */
            202: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["IssuedRequest"];
                };
            };
            400: components["responses"]["BadRequest"];
            403: components["responses"]["Forbidden"];
            409: components["responses"]["EmailRequestSuperseded"];
            429: components["responses"]["EmailRateLimited"];
            500: components["responses"]["InternalServerError"];
            502: components["responses"]["EmailDeliveryFailed"];
            503: components["responses"]["EmailVerificationUnavailable"];
        };
    };
    confirm: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["EmailVerificationConfirmRequest"];
            };
        };
        responses: {
            /** @description 이메일에 바인딩된 가입용 증명 발급. Cache-Control: no-store */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["EmailVerificationConfirmResponse"];
                };
            };
            400: components["responses"]["EmailCodeInvalid"];
            403: components["responses"]["Forbidden"];
            409: components["responses"]["EmailVerificationConflict"];
            410: components["responses"]["EmailCodeExpired"];
            429: components["responses"]["EmailCodeAttemptsExceeded"];
            500: components["responses"]["InternalServerError"];
            503: components["responses"]["EmailConfirmationUnavailable"];
        };
    };
    login: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["LoginRequest"];
            };
        };
        responses: {
            /** @description Access Token과 Refresh 쿠키 발급. Cache-Control: no-store */
            200: {
                headers: {
                    /** @description sayroam_refresh: HttpOnly, SameSite=Strict, Path=/api/auth, 기본 Secure. local 프로필의 HTTP 개발 환경에서만 Secure 비활성화 */
                    "Set-Cookie"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["LoginResponse"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["LoginUnauthorized"];
            403: components["responses"]["Forbidden"];
            500: components["responses"]["InternalServerError"];
            503: components["responses"]["AuthenticationUnavailable"];
        };
    };
    logout: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: {
                /** @description 브라우저가 자동 전송하는 HttpOnly 쿠키. 없는 경우에도 CSRF 검증 후 204로 브라우저 정리 */
                sayroam_refresh?: string;
            };
        };
        requestBody?: never;
        responses: {
            /** @description 현재 로그인의 후속 갱신 차단 및 Refresh 쿠키 만료. 본문 없음. 쿠키가 없거나 유효하지 않으면 브라우저 정리만 수행. Cache-Control: no-store */
            204: {
                headers: {
                    /** @description sayroam_refresh=; Path=/api/auth; Max-Age=0; HttpOnly; SameSite=Strict. 발급과 같은 Secure 설정 */
                    "Set-Cookie"?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
            401: components["responses"]["LogoutCookieInvalid"];
            403: components["responses"]["Forbidden"];
            500: components["responses"]["InternalServerError"];
            503: components["responses"]["LogoutUnavailable"];
        };
    };
    change: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PasswordChangeRequest"];
            };
        };
        responses: {
            /** @description 비밀번호 변경과 기존 로그인 무효화 완료. Cache-Control: no-store. 재로그인 필요 */
            204: {
                headers: {
                    /** @description 발급과 동일한 속성으로 sayroam_refresh 만료. XSRF-TOKEN은 유지 */
                    "Set-Cookie"?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
            400: components["responses"]["PasswordChangeBadRequest"];
            401: components["responses"]["AccountAuthenticationRevoked"];
            403: components["responses"]["PasswordChangeForbidden"];
            409: components["responses"]["PasswordChangeConflict"];
            410: components["responses"]["AccountProofExpired"];
            500: components["responses"]["InternalServerError"];
            503: components["responses"]["PasswordChangeUnavailable"];
        };
    };
    refresh: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 새 Access Token과 교체된 Refresh 쿠키. Cache-Control: no-store */
            200: {
                headers: {
                    /** @description sayroam_refresh 교체. 남은 절대 유효기간만 Max-Age로 지정 */
                    "Set-Cookie"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["LoginResponse"];
                };
            };
            401: components["responses"]["RefreshUnauthorized"];
            403: components["responses"]["Forbidden"];
            500: components["responses"]["InternalServerError"];
            503: components["responses"]["AuthenticationUnavailable"];
        };
    };
    signup: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SignupRequest"];
            };
        };
        responses: {
            /** @description 회원 생성 및 DB 커밋 완료. Cache-Control: no-store */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SignupResponse"];
                };
            };
            400: components["responses"]["SignupBadRequest"];
            403: components["responses"]["Forbidden"];
            409: components["responses"]["SignupConflict"];
            410: components["responses"]["SignupProofExpired"];
            500: components["responses"]["InternalServerError"];
            503: components["responses"]["SignupUnavailable"];
        };
    };
    startSocialLogin: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                provider: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SocialAuthStartRequest"];
            };
        };
        responses: {
            /** @description 인증 주소와 유효기간. Cache-Control: no-store */
            200: {
                headers: {
                    /** @description __Host-sayroam_oauth: HttpOnly, Secure, SameSite=Lax, Path=/. HTTP local에서는 sayroam_oauth이며 Secure=false */
                    "Set-Cookie"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SocialAuthStartResponse"];
                };
            };
            400: components["responses"]["SocialBadRequest"];
            401: components["responses"]["SocialUnauthorized"];
            403: components["responses"]["Forbidden"];
            409: components["responses"]["SocialConflict"];
            500: components["responses"]["InternalServerError"];
            503: components["responses"]["SocialUnavailable"];
        };
    };
    socialLoginCallback: {
        parameters: {
            query?: {
                code?: string;
                error?: string;
                state?: string;
            };
            header?: never;
            path: {
                provider: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 허용한 화면으로 이동. URL에 Access/Refresh Token 없음 */
            302: {
                headers: {
                    Location?: string;
                    [name: string]: unknown;
                };
                content?: never;
            };
            400: components["responses"]["SocialBadRequest"];
            401: components["responses"]["SocialUnauthorized"];
            403: components["responses"]["Forbidden"];
            409: components["responses"]["SocialConflict"];
            500: components["responses"]["InternalServerError"];
            503: components["responses"]["SocialUnavailable"];
        };
    };
    exchangeSocialLoginResult: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SocialResultExchangeRequest"];
            };
        };
        responses: {
            /** @description 로그인 또는 가입 대기. Cache-Control: no-store */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SocialExchangeResponse"];
                };
            };
            400: components["responses"]["SocialBadRequest"];
            401: components["responses"]["SocialUnauthorized"];
            403: components["responses"]["Forbidden"];
            409: components["responses"]["SocialConflict"];
            500: components["responses"]["InternalServerError"];
            503: components["responses"]["SocialUnavailable"];
        };
    };
    completeSocialSignup: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SocialSignupRequest"];
            };
        };
        responses: {
            /** @description 가입 커밋 후 Access 본문과 Refresh 쿠키 발급. Cache-Control: no-store */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["LoginResponse"];
                };
            };
            400: components["responses"]["SocialBadRequest"];
            401: components["responses"]["SocialUnauthorized"];
            403: components["responses"]["Forbidden"];
            409: components["responses"]["SocialConflict"];
            410: components["responses"]["SignupProofExpired"];
            500: components["responses"]["InternalServerError"];
            503: components["responses"]["SocialUnavailable"];
        };
    };
    create: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateRoomRequest"];
            };
        };
        responses: {
            /** @description 방과 방장 멤버 생성 완료 */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CreateRoomResponse"];
                };
            };
            400: components["responses"]["RoomBadRequest"];
            401: components["responses"]["Unauthorized"];
            500: components["responses"]["InternalServerError"];
            503: components["responses"]["AuthenticationUnavailable"];
        };
    };
    me: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 현재 회원 정보. Cache-Control: no-store */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CurrentUserResponse"];
                };
            };
            401: components["responses"]["Unauthorized"];
            500: components["responses"]["InternalServerError"];
            503: components["responses"]["AuthenticationUnavailable"];
        };
    };
}

/** operationId별 공통 실행 정보. 브라우저 이동용 콜백은 제외합니다. 직접 수정하지 않습니다. */
export const apiRoutes = {
    change: {
        method: "patch",
        path: "/api/auth/password",
        auth: "required",
        csrf: true,
        responses: { "204": false },
    },
    completeSocialSignup: {
        method: "post",
        path: "/api/auth/social/signup",
        auth: "none",
        csrf: true,
        responses: { "200": true },
    },
    confirm: {
        method: "post",
        path: "/api/auth/email-verifications/verify",
        auth: "none",
        csrf: true,
        responses: { "200": true },
    },
    confirm_1: {
        method: "post",
        path: "/api/auth/account-verifications/verify",
        auth: "required",
        csrf: true,
        responses: { "200": true },
    },
    create: {
        method: "post",
        path: "/api/rooms",
        auth: "required",
        csrf: false,
        responses: { "201": true },
    },
    csrf: {
        method: "get",
        path: "/api/auth/csrf",
        auth: "none",
        csrf: false,
        responses: { "204": false },
    },
    delete: {
        method: "post",
        path: "/api/auth/account-deletion",
        auth: "required",
        csrf: true,
        responses: { "204": false },
    },
    exchangeSocialLoginResult: {
        method: "post",
        path: "/api/auth/social/results/exchange",
        auth: "none",
        csrf: true,
        responses: { "200": true },
    },
    login: {
        method: "post",
        path: "/api/auth/login",
        auth: "none",
        csrf: true,
        responses: { "200": true },
    },
    logout: {
        method: "post",
        path: "/api/auth/logout",
        auth: "none",
        csrf: true,
        responses: { "204": false },
    },
    me: {
        method: "get",
        path: "/api/users/me",
        auth: "required",
        csrf: false,
        responses: { "200": true },
    },
    refresh: {
        method: "post",
        path: "/api/auth/refresh",
        auth: "none",
        csrf: true,
        responses: { "200": true },
    },
    request: {
        method: "post",
        path: "/api/auth/email-verifications",
        auth: "none",
        csrf: true,
        responses: { "202": true },
    },
    request_1: {
        method: "post",
        path: "/api/auth/account-verifications",
        auth: "required",
        csrf: true,
        responses: { "202": true },
    },
    signup: {
        method: "post",
        path: "/api/auth/signup",
        auth: "none",
        csrf: true,
        responses: { "201": true },
    },
    startSocialLogin: {
        method: "post",
        path: "/api/auth/social/{provider}/authorizations",
        auth: "none",
        csrf: true,
        responses: { "200": true },
    },
} as const satisfies Record<
    Exclude<keyof operations, "socialLoginCallback">,
    {
        method: string;
        path: keyof paths;
        auth: "none" | "required";
        csrf: boolean;
        responses: Readonly<Record<string, boolean>>;
    }
>;
