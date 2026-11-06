export type ClientOptions = {
    baseUrl: `${string}://${string}` | (string & {});
};
export type AccountDeletionRequest = {
    /**
     * ACCOUNT_DELETION 본인 확인에서 발급한 일회성 증명
     */
    accountProof: string;
};
export type AccountVerificationConfirmRequest = {
    /**
     * 메일로 받은 6자리 숫자 문자열
     */
    code: string;
    /**
     * 발급 시와 같은 목적
     */
    purpose: "PASSWORD_CHANGE" | "ACCOUNT_DELETION";
    /**
     * 발급 API가 반환한 verificationId
     */
    verificationId: string;
};
export type AccountVerificationConfirmResponse = {
    /**
     * 회원·현재 이메일·목적에 묶인 일회성 난수 증명. 가입용 증명·로그인 토큰이 아님
     */
    accountProof: string;
    /**
     * 증명 만료까지 남은 초
     */
    expiresInSeconds: number;
    purpose: "PASSWORD_CHANGE" | "ACCOUNT_DELETION";
};
export type AccountVerificationRequest = {
    /**
     * 본인 확인 목적. 회원·이메일은 인증된 계정에서 결정
     */
    purpose: "PASSWORD_CHANGE" | "ACCOUNT_DELETION";
};
export type AccountVerificationResponse = {
    /**
     * 코드 만료까지 남은 초
     */
    expiresInSeconds: number;
    purpose: "PASSWORD_CHANGE" | "ACCOUNT_DELETION";
    /**
     * 같은 회원이 어느 목적이든 다시 발송하기 전 대기 초
     */
    resendAfterSeconds: number;
    /**
     * 확인 요청을 식별하는 UUID
     */
    verificationId: string;
};
/**
 * 백엔드에 정의된 업무·공통 오류 코드. Spring 기본 요청 오류와 향후 추가 코드는 별도 문자열로 받을 수 있습니다.
 */
export type ApiErrorCode =
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
    | "UNAUTHENTICATED";
export type ApiProblem = {
    code: ApiErrorCode | string;
    detail: string;
    errors?: Array<FieldError>;
    instance: string;
    /**
     * 재시도 대기가 있는 429 응답에만 포함하는 대기 초. Retry-After 헤더와 같은 값
     */
    retryAfterSeconds?: number;
    status: number;
    title: string;
    /**
     * 생략하면 about:blank로 해석합니다.
     */
    type?: string;
};
export type CreateRoomRequest = {
    /**
     * 방 이름. 선택값이며 앞뒤 공백 제거 후 20자 이내. 공백뿐이면 이름 없는 방으로 저장
     */
    name?: string | null;
};
export type CreateRoomResponse = {
    /**
     * 생성된 방 ID
     */
    roomId: number;
};
export type CurrentUserResponse = {
    email: string | null;
    nickname: string;
    profileImageUrl: string | null;
    role: "USER" | "ADMIN";
    userId: number;
};
export type EmailRequest = {
    /**
     * 앞뒤 공백 제거 후 ASCII 이메일을 소문자로 정규화. 가입·로그인에도 같은 기준 적용 예정
     */
    email: string;
};
export type EmailVerificationConfirmRequest = {
    /**
     * 메일로 받은 6자리 숫자. 앞자리 0을 보존하는 문자열
     */
    code: string;
    /**
     * 발급 시와 같은 이메일. 공백 제거 후 소문자로 정규화
     */
    email: string;
    /**
     * 발급 API가 반환한 verificationId
     */
    verificationId: string;
};
export type EmailVerificationConfirmResponse = {
    /**
     * 가입용 증명이 만료될 때까지 남은 초
     */
    expiresInSeconds: number;
    /**
     * 이 이메일의 가입에만 한 번 사용할 난수 증명. JWT·로그인 토큰이 아님
     */
    signupProof: string;
};
export type FieldError = {
    field: string;
    reason: string;
};
export type IssuedRequest = {
    expiresInSeconds: number;
    resendAfterSeconds: number;
    verificationId: string;
};
export type LoginRequest = {
    /**
     * 가입한 이메일. 앞뒤 공백 제거 후 소문자로 정규화
     */
    email: string;
};
export type LoginResponse = {
    /**
     * API 요청의 Authorization 헤더로 전달하는 Access Token
     */
    accessToken: string;
    /**
     * 토큰 유효기간(초)
     */
    expiresIn: number;
    /**
     * 인증 방식
     */
    tokenType: "Bearer";
};
export type PasswordChangeRequest = {
    /**
     * PASSWORD_CHANGE 본인 확인에서 발급한 일회성 증명
     */
    accountProof: string;
    /**
     * 가입과 같은 8~128자 정책. 공백 제거·대소문자 변환 없이 해시 처리
     */
    newPassword: string;
};
export type SignupRequest = {
    /**
     * 인증한 이메일. 공백 제거 후 소문자로 정규화
     */
    email: string;
    /**
     * 앞뒤 공백 제거 후 1~50자. 제어 문자 불가
     */
    nickname: string;
};
export type SignupResponse = {
    /**
     * 생성된 회원 ID. 가입은 자동 로그인을 수행하지 않음
     */
    userId: number;
};
export type LoginRequestWritable = {
    /**
     * 가입한 이메일. 앞뒤 공백 제거 후 소문자로 정규화
     */
    email: string;
    /**
     * 가입한 비밀번호. 공백·대소문자를 그대로 검증
     */
    password: string;
};
export type SignupRequestWritable = {
    /**
     * 인증한 이메일. 공백 제거 후 소문자로 정규화
     */
    email: string;
    /**
     * 앞뒤 공백 제거 후 1~50자. 제어 문자 불가
     */
    nickname: string;
    /**
     * 8~128자. 공백 제거·대소문자 변환 없이 해시 처리
     */
    password: string;
    /**
     * 코드 검증 API에서 실제로 받은 43자 가입용 증명
     */
    signupProof: string;
};
export type DeleteData = {
    body: AccountDeletionRequest;
    path?: never;
    query?: never;
    url: "/api/auth/account-deletion";
};
export type DeleteErrors = {
    /**
     * 400: INVALID_INPUT, ACCOUNT_PROOF_INVALID 또는 Spring 요청 오류. 현재 회원의 ACCOUNT_DELETION 증명 필요
     */
    400: ApiProblem;
    /**
     * 401: UNAUTHENTICATED 또는 AUTHENTICATION_REVOKED. 현재 DB 인증 버전과 다른 로그인은 거절. 재로그인 필요. WWW-Authenticate: Bearer
     */
    401: ApiProblem;
    /**
     * 403: ACCESS_DENIED, INVALID_CSRF_TOKEN 또는 ACCOUNT_VERIFICATION_NOT_ALLOWED. 활성 계정의 인증된 대표 이메일이 필요
     */
    403: ApiProblem;
    /**
     * 409: ACCOUNT_PROOF_ALREADY_USED 또는 ACCOUNT_DELETION_CONFLICT. 소비한 증명은 복원하지 않음. 활성 계정이 남아 있을 때만 재로그인 후 새 본인 확인으로 재시도
     */
    409: ApiProblem;
    /**
     * 410: ACCOUNT_PROOF_EXPIRED. 없거나 만료된 증명 또는 현재 회원·이메일·목적·인증 버전의 증명이 없음. 새 본인 확인 필요
     */
    410: ApiProblem;
    /**
     * 500: INTERNAL_SERVER_ERROR
     */
    500: ApiProblem;
    /**
     * 503: AUTHENTICATION_UNAVAILABLE(사전 회원 조회), EMAIL_VERIFICATION_UNAVAILABLE(증명 소비 결과 불확실) 또는 ACCOUNT_DELETION_UNAVAILABLE(DB 커밋 결과 불확실). 마지막 코드일 때만 Refresh 쿠키 만료. 로컬 로그인 상태 정리 후 완료 여부 불확실 상태를 표시하며 자동 재전송·자동 재가입하지 않음
     */
    503: ApiProblem;
};
export type DeleteError = DeleteErrors[keyof DeleteErrors];
export type DeleteResponses = {
    /**
     * 탈퇴 및 계정 정리 완료. Cache-Control: no-store. 본문·새 토큰 없음
     */
    204: void;
};
export type DeleteResponse = DeleteResponses[keyof DeleteResponses];
export type Request1Data = {
    body: AccountVerificationRequest;
    path?: never;
    query?: never;
    url: "/api/auth/account-verifications";
};
export type Request1Errors = {
    /**
     * 400: INVALID_INPUT 또는 Spring 요청 오류. 검증 실패 시 errors 포함
     */
    400: ApiProblem;
    /**
     * 401: UNAUTHENTICATED 또는 AUTHENTICATION_REVOKED. 현재 DB 인증 버전과 다른 로그인은 거절. 재로그인 필요. WWW-Authenticate: Bearer
     */
    401: ApiProblem;
    /**
     * 403: ACCESS_DENIED, INVALID_CSRF_TOKEN 또는 ACCOUNT_VERIFICATION_NOT_ALLOWED. 활성 계정의 인증된 대표 이메일이 필요
     */
    403: ApiProblem;
    /**
     * 409: EMAIL_REQUEST_SUPERSEDED. 만료·교체된 요청이며 최신 요청을 사용해야 함
     */
    409: ApiProblem;
    /**
     * 429: EMAIL_RATE_LIMITED. Retry-After 헤더와 retryAfterSeconds 이후 재요청
     */
    429: ApiProblem;
    /**
     * 500: INTERNAL_SERVER_ERROR
     */
    500: ApiProblem;
    /**
     * 502: EMAIL_DELIVERY_FAILED. 코드 무효화, 발송 제한 유지. 자동 재시도 없음
     */
    502: ApiProblem;
    /**
     * 503: AUTHENTICATION_UNAVAILABLE, EMAIL_DELIVERY_DISABLED 또는 EMAIL_VERIFICATION_UNAVAILABLE. 회원 DB·SMTP 설정·Redis 문제. 발급 결과가 불확실하면 자동 재시도하지 않고 제한 시간 이후 새 코드 요청
     */
    503: ApiProblem;
};
export type Request1Error = Request1Errors[keyof Request1Errors];
export type Request1Responses = {
    /**
     * SMTP 서버에 발송 요청 완료. 수신함 도착을 보장하지 않음. Cache-Control: no-store
     */
    202: AccountVerificationResponse;
};
export type Request1Response = Request1Responses[keyof Request1Responses];
export type Confirm1Data = {
    body: AccountVerificationConfirmRequest;
    path?: never;
    query?: never;
    url: "/api/auth/account-verifications/verify";
};
export type Confirm1Errors = {
    /**
     * 400: INVALID_INPUT, EMAIL_CODE_INVALID 또는 Spring 요청 오류. 형식 오류는 수정, 코드 불일치는 남은 횟수 안에서 재입력
     */
    400: ApiProblem;
    /**
     * 401: UNAUTHENTICATED 또는 AUTHENTICATION_REVOKED. 현재 DB 인증 버전과 다른 로그인은 거절. 재로그인 필요. WWW-Authenticate: Bearer
     */
    401: ApiProblem;
    /**
     * 403: ACCESS_DENIED, INVALID_CSRF_TOKEN 또는 ACCOUNT_VERIFICATION_NOT_ALLOWED. 활성 계정의 인증된 대표 이메일이 필요
     */
    403: ApiProblem;
    /**
     * 409: EMAIL_REQUEST_SUPERSEDED 또는 EMAIL_CODE_ALREADY_USED. 최신 요청 또는 이미 받은 증명을 사용
     */
    409: ApiProblem;
    /**
     * 410: EMAIL_CODE_EXPIRED. 없거나 만료된 코드. 새 코드 발급 필요
     */
    410: ApiProblem;
    /**
     * 429: EMAIL_CODE_ATTEMPTS_EXCEEDED. 이 코드는 더 사용할 수 없으며 새 코드 발급 필요. Retry-After는 제공하지 않음
     */
    429: ApiProblem;
    /**
     * 500: INTERNAL_SERVER_ERROR
     */
    500: ApiProblem;
    /**
     * 503: AUTHENTICATION_UNAVAILABLE 또는 EMAIL_VERIFICATION_UNAVAILABLE. 코드 소비·증명 발급 결과가 불확실할 수 있어 자동 재시도하지 않고 새 코드 요청. SMTP 없이 기존 코드 확인 가능
     */
    503: ApiProblem;
};
export type Confirm1Error = Confirm1Errors[keyof Confirm1Errors];
export type Confirm1Responses = {
    /**
     * 목적별 일회성 accountProof. Cache-Control: no-store
     */
    200: AccountVerificationConfirmResponse;
};
export type Confirm1Response = Confirm1Responses[keyof Confirm1Responses];
export type CsrfData = {
    body?: never;
    path?: never;
    query?: never;
    url: "/api/auth/csrf";
};
export type CsrfResponses = {
    /**
     * CSRF 쿠키 준비 완료 (Cache-Control: no-store)
     */
    204: void;
};
export type CsrfResponse = CsrfResponses[keyof CsrfResponses];
export type RequestData = {
    body: EmailRequest;
    path?: never;
    query?: never;
    url: "/api/auth/email-verifications";
};
export type RequestErrors = {
    /**
     * 400: INVALID_INPUT 또는 Spring 요청 오류. 검증 실패 시 errors 포함
     */
    400: ApiProblem;
    /**
     * 403: ACCESS_DENIED 또는 INVALID_CSRF_TOKEN
     */
    403: ApiProblem;
    /**
     * 409: EMAIL_REQUEST_SUPERSEDED. 만료·교체된 요청이며 최신 요청을 사용해야 함
     */
    409: ApiProblem;
    /**
     * 429: EMAIL_RATE_LIMITED. Retry-After 헤더와 retryAfterSeconds 이후 재요청
     */
    429: ApiProblem;
    /**
     * 500: INTERNAL_SERVER_ERROR
     */
    500: ApiProblem;
    /**
     * 502: EMAIL_DELIVERY_FAILED. 코드 무효화, 발송 제한 유지. 자동 재시도 없음
     */
    502: ApiProblem;
    /**
     * 503: EMAIL_DELIVERY_DISABLED 또는 EMAIL_VERIFICATION_UNAVAILABLE. 설정·저장소 복구 후 재요청
     */
    503: ApiProblem;
};
export type RequestError = RequestErrors[keyof RequestErrors];
export type RequestResponses = {
    /**
     * SMTP 서버에 발송 요청 완료. 수신함 도착을 보장하지 않음
     */
    202: IssuedRequest;
};
export type RequestResponse = RequestResponses[keyof RequestResponses];
export type ConfirmData = {
    body: EmailVerificationConfirmRequest;
    path?: never;
    query?: never;
    url: "/api/auth/email-verifications/verify";
};
export type ConfirmErrors = {
    /**
     * 400: INVALID_INPUT, EMAIL_CODE_INVALID 또는 Spring 요청 오류. 형식 오류는 수정, 코드 불일치는 남은 횟수 안에서 재입력
     */
    400: ApiProblem;
    /**
     * 403: ACCESS_DENIED 또는 INVALID_CSRF_TOKEN
     */
    403: ApiProblem;
    /**
     * 409: EMAIL_REQUEST_SUPERSEDED 또는 EMAIL_CODE_ALREADY_USED. 최신 요청 또는 이미 받은 증명을 사용
     */
    409: ApiProblem;
    /**
     * 410: EMAIL_CODE_EXPIRED. 없거나 만료된 코드. 새 코드 발급 필요
     */
    410: ApiProblem;
    /**
     * 429: EMAIL_CODE_ATTEMPTS_EXCEEDED. 이 코드는 더 사용할 수 없으며 새 코드 발급 필요. Retry-After는 제공하지 않음
     */
    429: ApiProblem;
    /**
     * 500: INTERNAL_SERVER_ERROR
     */
    500: ApiProblem;
    /**
     * 503: EMAIL_VERIFICATION_UNAVAILABLE. Redis 작업 결과가 불확실할 수 있음. 코드가 소비됐으면 새 코드 발급 필요
     */
    503: ApiProblem;
};
export type ConfirmError = ConfirmErrors[keyof ConfirmErrors];
export type ConfirmResponses = {
    /**
     * 이메일에 바인딩된 가입용 증명 발급. Cache-Control: no-store
     */
    200: EmailVerificationConfirmResponse;
};
export type ConfirmResponse = ConfirmResponses[keyof ConfirmResponses];
export type LoginData = {
    body: LoginRequestWritable;
    path?: never;
    query?: never;
    url: "/api/auth/login";
};
export type LoginErrors = {
    /**
     * 400: INVALID_INPUT 또는 Spring 요청 오류. 검증 실패 시 errors 포함
     */
    400: ApiProblem;
    /**
     * 401: INVALID_CREDENTIALS. 이메일 없음·비밀번호 불일치·탈퇴·이메일 미인증·비밀번호 미등록에 동일한 응답. 불필요한 Authorization 헤더가 있으면 필터에서 UNAUTHENTICATED로 거절될 수 있음
     */
    401: ApiProblem;
    /**
     * 403: ACCESS_DENIED 또는 INVALID_CSRF_TOKEN
     */
    403: ApiProblem;
    /**
     * 500: INTERNAL_SERVER_ERROR
     */
    500: ApiProblem;
    /**
     * 503: AUTHENTICATION_UNAVAILABLE. 회원 DB 또는 Refresh Redis 저장소 장애. 성공 응답 없이 거절하며 갱신 결과가 불확실할 수 있으므로 자동 재시도하지 않음
     */
    503: ApiProblem;
};
export type LoginError = LoginErrors[keyof LoginErrors];
export type LoginResponses = {
    /**
     * Access Token과 Refresh 쿠키 발급. Cache-Control: no-store
     */
    200: LoginResponse;
};
export type LoginResponse2 = LoginResponses[keyof LoginResponses];
export type LogoutData = {
    body?: never;
    path?: never;
    query?: never;
    url: "/api/auth/logout";
};
export type LogoutErrors = {
    /**
     * 401: REFRESH_TOKEN_INVALID. 같은 이름의 Refresh 쿠키가 중복되어 폐기 대상을 선택할 수 없음. 발급 경로의 쿠키만 만료시키며 다른 경로·호스트의 중복 쿠키는 브라우저에서 정리해야 함
     */
    401: ApiProblem;
    /**
     * 403: ACCESS_DENIED 또는 INVALID_CSRF_TOKEN
     */
    403: ApiProblem;
    /**
     * 500: INTERNAL_SERVER_ERROR
     */
    500: ApiProblem;
    /**
     * 503: AUTHENTICATION_UNAVAILABLE. Redis 폐기 결과를 확인할 수 없음. 성공 응답·Refresh 쿠키 삭제 없이 실패하며 같은 쿠키로 재시도 가능. 폐기는 이미 실행됐을 수도 있음
     */
    503: ApiProblem;
};
export type LogoutError = LogoutErrors[keyof LogoutErrors];
export type LogoutResponses = {
    /**
     * 현재 로그인의 후속 갱신 차단 및 Refresh 쿠키 만료. 본문 없음. 쿠키가 없거나 유효하지 않으면 브라우저 정리만 수행. Cache-Control: no-store
     */
    204: void;
};
export type LogoutResponse = LogoutResponses[keyof LogoutResponses];
export type ChangeData = {
    body: PasswordChangeRequest;
    path?: never;
    query?: never;
    url: "/api/auth/password";
};
export type ChangeErrors = {
    /**
     * 400: INVALID_INPUT, PASSWORD_POLICY_VIOLATION, ACCOUNT_PROOF_INVALID 또는 Spring 요청 오류. PASSWORD_CHANGE 증명과 8~128자의 새 비밀번호 필요. 비밀번호 정책 실패는 증명을 소비하지 않음
     */
    400: ApiProblem;
    /**
     * 401: UNAUTHENTICATED 또는 AUTHENTICATION_REVOKED. 현재 DB 인증 버전과 다른 로그인은 거절. 재로그인 필요. WWW-Authenticate: Bearer
     */
    401: ApiProblem;
    /**
     * 403: ACCESS_DENIED, INVALID_CSRF_TOKEN, ACCOUNT_VERIFICATION_NOT_ALLOWED 또는 PASSWORD_CHANGE_NOT_ALLOWED. 활성·이메일 인증·기존 비밀번호 등록 계정만 허용. 소셜 전용 계정의 비밀번호 등록은 지원하지 않음
     */
    403: ApiProblem;
    /**
     * 409: ACCOUNT_PROOF_ALREADY_USED 또는 PASSWORD_CHANGE_CONFLICT. 소비된 증명 복원 없음. 회원 상태·이메일·인증 버전 충돌 시 재로그인 후 새 본인 확인 필요
     */
    409: ApiProblem;
    /**
     * 410: ACCOUNT_PROOF_EXPIRED. 없거나 만료된 증명 또는 현재 회원·이메일·목적·인증 버전의 증명이 없음. 새 본인 확인 필요
     */
    410: ApiProblem;
    /**
     * 500: INTERNAL_SERVER_ERROR
     */
    500: ApiProblem;
    /**
     * 503: AUTHENTICATION_UNAVAILABLE(사전 DB 조회), EMAIL_VERIFICATION_UNAVAILABLE(증명 소비 결과 불확실) 또는 PASSWORD_CHANGE_UNAVAILABLE(DB 변경·커밋 결과 불확실). 자동 재시도 금지. 마지막 코드일 때만 Refresh 쿠키를 만료시키며 프론트는 로그인 상태를 지우고 재로그인으로 결과 확인
     */
    503: ApiProblem;
};
export type ChangeError = ChangeErrors[keyof ChangeErrors];
export type ChangeResponses = {
    /**
     * 비밀번호 변경과 기존 로그인 무효화 완료. Cache-Control: no-store. 재로그인 필요
     */
    204: void;
};
export type ChangeResponse = ChangeResponses[keyof ChangeResponses];
export type RefreshData = {
    body?: never;
    path?: never;
    query?: never;
    url: "/api/auth/refresh";
};
export type RefreshErrors = {
    /**
     * 401: REFRESH_TOKEN_INVALID 또는 REFRESH_TOKEN_REUSED. 쿠키 누락·중복·형식 오류·만료·폐기·회원 삭제/탈퇴·교체 한도, 또는 사용한 토큰 재사용. Refresh 쿠키를 만료시키며 재로그인 필요. 재사용은 해당 로그인만 갱신 불가로 만듦
     */
    401: ApiProblem;
    /**
     * 403: ACCESS_DENIED 또는 INVALID_CSRF_TOKEN
     */
    403: ApiProblem;
    /**
     * 500: INTERNAL_SERVER_ERROR
     */
    500: ApiProblem;
    /**
     * 503: AUTHENTICATION_UNAVAILABLE. 회원 DB 또는 Refresh Redis 저장소 장애. 성공 응답 없이 거절하며 갱신 결과가 불확실할 수 있으므로 자동 재시도하지 않음
     */
    503: ApiProblem;
};
export type RefreshError = RefreshErrors[keyof RefreshErrors];
export type RefreshResponses = {
    /**
     * 새 Access Token과 교체된 Refresh 쿠키. Cache-Control: no-store
     */
    200: LoginResponse;
};
export type RefreshResponse = RefreshResponses[keyof RefreshResponses];
export type SignupData = {
    body: SignupRequestWritable;
    path?: never;
    query?: never;
    url: "/api/auth/signup";
};
export type SignupErrors = {
    /**
     * 400: INVALID_INPUT, PASSWORD_POLICY_VIOLATION, SIGNUP_PROOF_INVALID 또는 Spring 요청 오류. 입력·비밀번호 정책 검증 실패는 증명을 소비하지 않음
     */
    400: ApiProblem;
    /**
     * 403: ACCESS_DENIED 또는 INVALID_CSRF_TOKEN
     */
    403: ApiProblem;
    /**
     * 409: SIGNUP_PROOF_ALREADY_USED 또는 EMAIL_ALREADY_REGISTERED. 이미 소비한 증명은 복구하지 않음
     */
    409: ApiProblem;
    /**
     * 410: SIGNUP_PROOF_EXPIRED. 없거나 만료된 증명. 새 이메일 인증 필요
     */
    410: ApiProblem;
    /**
     * 500: INTERNAL_SERVER_ERROR
     */
    500: ApiProblem;
    /**
     * 503: EMAIL_VERIFICATION_UNAVAILABLE 또는 SIGNUP_UNAVAILABLE. 증명 소비·DB 커밋 결과가 불확실할 수 있음. 자동 재시도 없이 새 인증 필요; 계정은 이미 생성됐을 수도 있음
     */
    503: ApiProblem;
};
export type SignupError = SignupErrors[keyof SignupErrors];
export type SignupResponses = {
    /**
     * 회원 생성 및 DB 커밋 완료. Cache-Control: no-store
     */
    201: SignupResponse;
};
export type SignupResponse2 = SignupResponses[keyof SignupResponses];
export type CreateData = {
    body: CreateRoomRequest;
    path?: never;
    query?: never;
    url: "/api/rooms";
};
export type CreateErrors = {
    /**
     * 400: ROOM_NAME_TOO_LONG 또는 Spring 요청 오류. 이름을 20자 이내로 수정 후 재요청
     */
    400: ApiProblem;
    /**
     * 401: UNAUTHENTICATED. 토큰 누락·만료·변조·잘못된 발급자/대상·유형·인증 버전 또는 탈퇴·삭제 회원. WWW-Authenticate: Bearer
     */
    401: ApiProblem;
    /**
     * 500: INTERNAL_SERVER_ERROR
     */
    500: ApiProblem;
    /**
     * 503: AUTHENTICATION_UNAVAILABLE. 회원 DB 또는 Refresh Redis 저장소 장애. 성공 응답 없이 거절하며 갱신 결과가 불확실할 수 있으므로 자동 재시도하지 않음
     */
    503: ApiProblem;
};
export type CreateError = CreateErrors[keyof CreateErrors];
export type CreateResponses = {
    /**
     * 방과 방장 멤버 생성 완료
     */
    201: CreateRoomResponse;
};
export type CreateResponse = CreateResponses[keyof CreateResponses];
export type MeData = {
    body?: never;
    path?: never;
    query?: never;
    url: "/api/users/me";
};
export type MeErrors = {
    /**
     * 401: UNAUTHENTICATED. 토큰 누락·만료·변조·잘못된 발급자/대상·유형·인증 버전 또는 탈퇴·삭제 회원. WWW-Authenticate: Bearer
     */
    401: ApiProblem;
    /**
     * 500: INTERNAL_SERVER_ERROR
     */
    500: ApiProblem;
    /**
     * 503: AUTHENTICATION_UNAVAILABLE. 회원 DB 또는 Refresh Redis 저장소 장애. 성공 응답 없이 거절하며 갱신 결과가 불확실할 수 있으므로 자동 재시도하지 않음
     */
    503: ApiProblem;
};
export type MeError = MeErrors[keyof MeErrors];
export type MeResponses = {
    /**
     * 현재 회원 정보. Cache-Control: no-store
     */
    200: CurrentUserResponse;
};
export type MeResponse = MeResponses[keyof MeResponses];
