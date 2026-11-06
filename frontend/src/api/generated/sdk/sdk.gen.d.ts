import type {
    Client,
    ClientMeta,
    Options as Options2,
    RequestResult,
    TDataShape,
} from "./client/index.js";
import type {
    ChangeData,
    ChangeErrors,
    ChangeResponses,
    Confirm1Data,
    Confirm1Errors,
    Confirm1Responses,
    ConfirmData,
    ConfirmErrors,
    ConfirmResponses,
    CreateData,
    CreateErrors,
    CreateResponses,
    CsrfData,
    CsrfResponses,
    DeleteData,
    DeleteErrors,
    DeleteResponses,
    LoginData,
    LoginErrors,
    LoginResponses,
    LogoutData,
    LogoutErrors,
    LogoutResponses,
    MeData,
    MeErrors,
    MeResponses,
    RefreshData,
    RefreshErrors,
    RefreshResponses,
    Request1Data,
    Request1Errors,
    Request1Responses,
    RequestData,
    RequestErrors,
    RequestResponses,
    SignupData,
    SignupErrors,
    SignupResponses,
} from "./types.gen.js";
export type Options<
    TData extends TDataShape = TDataShape,
    ThrowOnError extends boolean = boolean,
    TResponse = unknown,
> = Options2<TData, ThrowOnError, TResponse> & {
    /**
     * You can provide a client instance returned by `createClient()` instead of
     * individual options. This might be also useful if you want to implement a
     * custom client.
     */
    client?: Client;
    /**
     * You can pass arbitrary values through the `meta` object. This can be
     * used to access values that aren't defined as part of the SDK function.
     */
    meta?: keyof ClientMeta extends never ? Record<string, unknown> : ClientMeta;
};
/**
 * 본인 회원 탈퇴·계정 프로필 및 로그인 수단 정리
 *
 * Bearer 인증과 CSRF 쿠키·X-XSRF-TOKEN 헤더 필요. ACCOUNT_DELETION 이메일 본인 확인의 accountProof만 한 번 소비합니다. 현재 회원의 탈퇴 상태·프로필 익명화·인증 버전 증가와 비밀번호·소셜 로그인 연결·개인 여행 성향·수신 알림 삭제를 한 DB 트랜잭션으로 커밋합니다. 회원 ID와 채팅·정산 등 공유 기록의 참조는 보존합니다. 모든 기기의 기존 로그인·Access 인증·Refresh 갱신을 차단하고 현재 Refresh 쿠키를 만료시킵니다. 같은 이메일로 재가입하면 새 회원이며 기존 계정을 복원하지 않습니다. 응답 유실·DB 커밋 결과 불확실 시 자동 재전송·자동 재가입하지 마세요.
 */
export declare const delete_: <ThrowOnError extends boolean = true>(
    options: Options<DeleteData, ThrowOnError>,
) => RequestResult<DeleteResponses, DeleteErrors, ThrowOnError, "data">;
/**
 * 계정 관리용 이메일 본인 확인 코드 발급·재발송
 *
 * Bearer 인증과 CSRF 쿠키·X-XSRF-TOKEN 헤더가 모두 필요합니다. DB의 현재 인증된 대표 이메일로만 발송합니다. PASSWORD_CHANGE/ACCOUNT_DELETION의 코드·증명은 분리하고 발송 제한은 합산합니다. 승인된 재발급은 같은 목적의 이전 코드·증명을 무효화합니다. 이 API는 비밀번호 변경·탈퇴를 실행하지 않습니다.
 */
export declare const request1: <ThrowOnError extends boolean = true>(
    options: Options<Request1Data, ThrowOnError>,
) => RequestResult<Request1Responses, Request1Errors, ThrowOnError, "data">;
/**
 * 계정 관리용 코드 검증·일회성 증명 발급
 *
 * 같은 회원·현재 이메일·목적·인증 버전·요청 ID·코드를 확인한 후 코드를 소비하고 accountProof를 한 번 반환합니다. 증명은 해당 목적의 작업에서 한 번만 소비할 수 있습니다. 가입용 signupProof와 호환되지 않습니다. 확인 응답을 잃으면 코드를 재사용하지 말고 새로 발급해야 합니다. PASSWORD_CHANGE 증명은 PATCH /api/auth/password, ACCOUNT_DELETION 증명은 POST /api/auth/account-deletion에서 사용합니다.
 */
export declare const confirm1: <ThrowOnError extends boolean = true>(
    options: Options<Confirm1Data, ThrowOnError>,
) => RequestResult<Confirm1Responses, Confirm1Errors, ThrowOnError, "data">;
/**
 * CSRF 쿠키 최초 발급
 *
 * 응답의 XSRF-TOKEN 쿠키 값을 로그인·가입·토큰 갱신·로그아웃 등 이후 변경 요청의 X-XSRF-TOKEN 헤더에 전달합니다. 토큰은 응답 본문에 넣지 않습니다. 로그인·갱신·로그아웃은 현재 CSRF 쿠키를 유지하며 쿠키가 없거나 만료됐으면 다시 준비합니다.
 */
export declare const csrf: <ThrowOnError extends boolean = true>(
    options?: Options<CsrfData, ThrowOnError>,
) => RequestResult<CsrfResponses, unknown, ThrowOnError, "data">;
/**
 * 이메일 인증 코드 발급·재발송
 *
 * CSRF 쿠키와 X-XSRF-TOKEN 헤더 필요. 코드는 6자리이며 응답에 포함하지 않습니다. 재발급 승인 시 이전 코드와 가입용 증명은 무효화합니다.
 */
export declare const request: <ThrowOnError extends boolean = true>(
    options: Options<RequestData, ThrowOnError>,
) => RequestResult<RequestResponses, RequestErrors, ThrowOnError, "data">;
/**
 * 이메일 인증 코드 검증 및 가입용 증명 발급
 *
 * 이메일·요청 ID·코드를 원자적으로 확인하고 코드를 소비합니다. 성공한 코드는 다시 사용할 수 없습니다. SMTP 설정 없이 기존 코드 검증은 가능하지만 Redis는 필요합니다. 가입용 증명은 POST /api/auth/signup에서 한 번 사용할 수 있습니다.
 */
export declare const confirm: <ThrowOnError extends boolean = true>(
    options: Options<ConfirmData, ThrowOnError>,
) => RequestResult<ConfirmResponses, ConfirmErrors, ThrowOnError, "data">;
/**
 * 이메일·비밀번호 로그인
 *
 * XSRF-TOKEN 쿠키와 X-XSRF-TOKEN 헤더 필요. 활성·이메일 인증 회원의 비밀번호를 확인해 Access Token을 본문에, Refresh Token을 HttpOnly 쿠키에 발급합니다. Redis 저장이 완료된 뒤 응답합니다. 이메일 없음·비밀번호 불일치·탈퇴·비밀번호 미등록은 같은 실패 응답입니다. Authorization 헤더 없이 호출하세요.
 */
export declare const login: <ThrowOnError extends boolean = true>(
    options: Options<LoginData, ThrowOnError>,
) => RequestResult<LoginResponses, LoginErrors, ThrowOnError, "data">;
/**
 * 현재 로그인 로그아웃
 *
 * XSRF-TOKEN 쿠키·X-XSRF-TOKEN 헤더가 필요합니다. sayroam_refresh 쿠키에 연결된 로그인만 갱신 불가로 만들고 쿠키를 만료시킵니다. Access Token은 필요하지 않으며 Authorization 헤더는 무시합니다. 쿠키 없음·형식 오류·만료·이미 폐기된 토큰도 204로 정리합니다. 같은 이름의 중복 쿠키는 401로 거절합니다. 이미 발급한 Access Token은 만료(기본 15분, 최대 30초 시간 오차)까지 유효하므로 클라이언트 메모리에서도 제거해야 합니다. 모든 기기 로그아웃·계정 삭제·비밀번호 변경은 포함하지 않습니다.
 */
export declare const logout: <ThrowOnError extends boolean = true>(
    options?: Options<LogoutData, ThrowOnError>,
) => RequestResult<LogoutResponses, LogoutErrors, ThrowOnError, "data">;
/**
 * 본인 비밀번호 변경·모든 기존 로그인 무효화
 *
 * Bearer 인증과 CSRF 쿠키·X-XSRF-TOKEN 헤더 필요. PASSWORD_CHANGE 이메일 본인 확인의 accountProof를 한 번 소비합니다. 기존 비밀번호가 등록된 본인 계정만 변경하며 소셜 전용 계정에 비밀번호를 새로 등록하지 않습니다. 해시·변경 시각·인증 버전 증가를 같은 DB 트랜잭션으로 커밋합니다. 모든 기기의 기존 Access/Refresh는 이후 인증·갱신에서 거절됩니다. 성공 시 현재 Refresh 쿠키를 만료시키며 프론트는 Access Token·회원 정보·진행 중 갱신을 정리하고 새 비밀번호로 재로그인해야 합니다. 응답 유실 시 자동 재전송하지 마세요.
 */
export declare const change: <ThrowOnError extends boolean = true>(
    options: Options<ChangeData, ThrowOnError>,
) => RequestResult<ChangeResponses, ChangeErrors, ThrowOnError, "data">;
/**
 * 토큰 갱신
 *
 * 브라우저의 sayroam_refresh HttpOnly 쿠키와 XSRF-TOKEN 쿠키·X-XSRF-TOKEN 헤더가 필요합니다. Access Token 없이 호출하며 이 경로에서는 Authorization 헤더를 인증에 사용하지 않습니다. 활성 회원을 확인한 뒤 Refresh Token을 일회성 교체합니다. 이전 값 재사용·동시 갱신은 해당 로그인의 갱신 권한을 폐기합니다. 클라이언트는 갱신 요청을 직렬화하고 불확실한 응답을 자동 재시도하지 않아야 합니다. 절대 만료는 연장하지 않습니다.
 */
export declare const refresh: <ThrowOnError extends boolean = true>(
    options?: Options<RefreshData, ThrowOnError>,
) => RequestResult<RefreshResponses, RefreshErrors, ThrowOnError, "data">;
/**
 * 이메일 인증 후 회원가입
 *
 * CSRF 쿠키와 X-XSRF-TOKEN 헤더 필요. 가입용 증명을 먼저 일회성 소비한 뒤 일반 회원과 비밀번호 해시를 함께 저장합니다. DB 실패 시 증명을 복구하지 않으며 새 이메일 인증이 필요합니다. 자동 로그인과 JWT 발급은 수행하지 않습니다.
 */
export declare const signup: <ThrowOnError extends boolean = true>(
    options: Options<SignupData, ThrowOnError>,
) => RequestResult<SignupResponses, SignupErrors, ThrowOnError, "data">;
/**
 * 채팅방 생성
 *
 * 로그인한 회원을 방장으로 하여 방을 생성합니다.
 */
export declare const create: <ThrowOnError extends boolean = true>(
    options: Options<CreateData, ThrowOnError>,
) => RequestResult<CreateResponses, CreateErrors, ThrowOnError, "data">;
/**
 * 내 회원 정보 조회
 *
 * Authorization: Bearer <accessToken> 필요. 토큰 검증 후 DB에서 조회한 현재 회원 정보를 반환합니다. 탈퇴·삭제 회원은 인증되지 않으며 요청에서 다른 회원의 ID를 받지 않습니다.
 */
export declare const me: <ThrowOnError extends boolean = true>(
    options?: Options<MeData, ThrowOnError>,
) => RequestResult<MeResponses, MeErrors, ThrowOnError, "data">;
