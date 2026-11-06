import * as v from "valibot";
export declare const vAccountDeletionRequest: v.ObjectSchema<
    {
        readonly accountProof: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.MinLengthAction<string, 1, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
    },
    undefined
>;
export declare const vAccountVerificationConfirmRequest: v.ObjectSchema<
    {
        readonly code: v.SchemaWithPipe<
            readonly [v.StringSchema<undefined>, v.RegexAction<string, undefined>]
        >;
        readonly purpose: v.PicklistSchema<["PASSWORD_CHANGE", "ACCOUNT_DELETION"], undefined>;
        readonly verificationId: v.SchemaWithPipe<
            readonly [v.StringSchema<undefined>, v.UuidAction<string, undefined>]
        >;
    },
    undefined
>;
export declare const vAccountVerificationConfirmResponse: v.ObjectSchema<
    {
        readonly accountProof: v.StringSchema<undefined>;
        readonly expiresInSeconds: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
        readonly purpose: v.PicklistSchema<["PASSWORD_CHANGE", "ACCOUNT_DELETION"], undefined>;
    },
    undefined
>;
export declare const vAccountVerificationRequest: v.ObjectSchema<
    {
        readonly purpose: v.PicklistSchema<["PASSWORD_CHANGE", "ACCOUNT_DELETION"], undefined>;
    },
    undefined
>;
export declare const vAccountVerificationResponse: v.ObjectSchema<
    {
        readonly expiresInSeconds: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
        readonly purpose: v.PicklistSchema<["PASSWORD_CHANGE", "ACCOUNT_DELETION"], undefined>;
        readonly resendAfterSeconds: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
        readonly verificationId: v.StringSchema<undefined>;
    },
    undefined
>;
/**
 * 백엔드에 정의된 업무·공통 오류 코드. Spring 기본 요청 오류와 향후 추가 코드는 별도 문자열로 받을 수 있습니다.
 */
export declare const vApiErrorCode: v.PicklistSchema<
    [
        "ACCESS_DENIED",
        "ACCOUNT_DELETION_CONFLICT",
        "ACCOUNT_DELETION_UNAVAILABLE",
        "ACCOUNT_PROOF_ALREADY_USED",
        "ACCOUNT_PROOF_EXPIRED",
        "ACCOUNT_PROOF_INVALID",
        "ACCOUNT_VERIFICATION_NOT_ALLOWED",
        "AUTHENTICATION_REVOKED",
        "AUTHENTICATION_UNAVAILABLE",
        "EMAIL_ALREADY_REGISTERED",
        "EMAIL_CODE_ALREADY_USED",
        "EMAIL_CODE_ATTEMPTS_EXCEEDED",
        "EMAIL_CODE_EXPIRED",
        "EMAIL_CODE_INVALID",
        "EMAIL_DELIVERY_DISABLED",
        "EMAIL_DELIVERY_FAILED",
        "EMAIL_RATE_LIMITED",
        "EMAIL_REQUEST_SUPERSEDED",
        "EMAIL_VERIFICATION_UNAVAILABLE",
        "INTERNAL_SERVER_ERROR",
        "INVALID_CREDENTIALS",
        "INVALID_CSRF_TOKEN",
        "INVALID_INPUT",
        "PASSWORD_CHANGE_CONFLICT",
        "PASSWORD_CHANGE_NOT_ALLOWED",
        "PASSWORD_CHANGE_UNAVAILABLE",
        "PASSWORD_POLICY_VIOLATION",
        "REFRESH_TOKEN_INVALID",
        "REFRESH_TOKEN_REUSED",
        "ROOM_NAME_TOO_LONG",
        "SIGNUP_PROOF_ALREADY_USED",
        "SIGNUP_PROOF_EXPIRED",
        "SIGNUP_PROOF_INVALID",
        "SIGNUP_UNAVAILABLE",
        "UNAUTHENTICATED",
    ],
    undefined
>;
export declare const vCreateRoomRequest: v.ObjectSchema<
    {
        readonly name: v.NullishSchema<
            v.SchemaWithPipe<
                readonly [v.StringSchema<undefined>, v.MaxLengthAction<string, 20, undefined>]
            >,
            undefined
        >;
    },
    undefined
>;
export declare const vCreateRoomResponse: v.ObjectSchema<
    {
        readonly roomId: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
    },
    undefined
>;
export declare const vCurrentUserResponse: v.ObjectSchema<
    {
        readonly email: v.NullableSchema<v.StringSchema<undefined>, undefined>;
        readonly nickname: v.StringSchema<undefined>;
        readonly profileImageUrl: v.NullableSchema<v.StringSchema<undefined>, undefined>;
        readonly role: v.PicklistSchema<["USER", "ADMIN"], undefined>;
        readonly userId: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
    },
    undefined
>;
export declare const vEmailRequest: v.ObjectSchema<
    {
        readonly email: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.EmailAction<string, undefined>,
                v.MinLengthAction<string, 0, undefined>,
                v.MaxLengthAction<string, 254, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
    },
    undefined
>;
export declare const vEmailVerificationConfirmRequest: v.ObjectSchema<
    {
        readonly code: v.SchemaWithPipe<
            readonly [v.StringSchema<undefined>, v.RegexAction<string, undefined>]
        >;
        readonly email: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.EmailAction<string, undefined>,
                v.MinLengthAction<string, 0, undefined>,
                v.MaxLengthAction<string, 254, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
        readonly verificationId: v.SchemaWithPipe<
            readonly [v.StringSchema<undefined>, v.UuidAction<string, undefined>]
        >;
    },
    undefined
>;
export declare const vEmailVerificationConfirmResponse: v.ObjectSchema<
    {
        readonly expiresInSeconds: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
        readonly signupProof: v.StringSchema<undefined>;
    },
    undefined
>;
export declare const vFieldError: v.ObjectSchema<
    {
        readonly field: v.StringSchema<undefined>;
        readonly reason: v.StringSchema<undefined>;
    },
    undefined
>;
export declare const vApiProblem: v.ObjectSchema<
    {
        readonly code: v.UnionSchema<
            [
                v.PicklistSchema<
                    [
                        "ACCESS_DENIED",
                        "ACCOUNT_DELETION_CONFLICT",
                        "ACCOUNT_DELETION_UNAVAILABLE",
                        "ACCOUNT_PROOF_ALREADY_USED",
                        "ACCOUNT_PROOF_EXPIRED",
                        "ACCOUNT_PROOF_INVALID",
                        "ACCOUNT_VERIFICATION_NOT_ALLOWED",
                        "AUTHENTICATION_REVOKED",
                        "AUTHENTICATION_UNAVAILABLE",
                        "EMAIL_ALREADY_REGISTERED",
                        "EMAIL_CODE_ALREADY_USED",
                        "EMAIL_CODE_ATTEMPTS_EXCEEDED",
                        "EMAIL_CODE_EXPIRED",
                        "EMAIL_CODE_INVALID",
                        "EMAIL_DELIVERY_DISABLED",
                        "EMAIL_DELIVERY_FAILED",
                        "EMAIL_RATE_LIMITED",
                        "EMAIL_REQUEST_SUPERSEDED",
                        "EMAIL_VERIFICATION_UNAVAILABLE",
                        "INTERNAL_SERVER_ERROR",
                        "INVALID_CREDENTIALS",
                        "INVALID_CSRF_TOKEN",
                        "INVALID_INPUT",
                        "PASSWORD_CHANGE_CONFLICT",
                        "PASSWORD_CHANGE_NOT_ALLOWED",
                        "PASSWORD_CHANGE_UNAVAILABLE",
                        "PASSWORD_POLICY_VIOLATION",
                        "REFRESH_TOKEN_INVALID",
                        "REFRESH_TOKEN_REUSED",
                        "ROOM_NAME_TOO_LONG",
                        "SIGNUP_PROOF_ALREADY_USED",
                        "SIGNUP_PROOF_EXPIRED",
                        "SIGNUP_PROOF_INVALID",
                        "SIGNUP_UNAVAILABLE",
                        "UNAUTHENTICATED",
                    ],
                    undefined
                >,
                v.StringSchema<undefined>,
            ],
            undefined
        >;
        readonly detail: v.StringSchema<undefined>;
        readonly errors: v.OptionalSchema<
            v.ArraySchema<
                v.ObjectSchema<
                    {
                        readonly field: v.StringSchema<undefined>;
                        readonly reason: v.StringSchema<undefined>;
                    },
                    undefined
                >,
                undefined
            >,
            undefined
        >;
        readonly instance: v.StringSchema<undefined>;
        readonly retryAfterSeconds: v.OptionalSchema<
            v.SchemaWithPipe<
                readonly [
                    v.NumberSchema<undefined>,
                    v.IntegerAction<number, undefined>,
                    v.MinValueAction<number, -9007199254740991, undefined>,
                    v.MaxValueAction<number, 9007199254740991, undefined>,
                ]
            >,
            undefined
        >;
        readonly status: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<
                    number,
                    -2147483648,
                    "Invalid value: Expected int32 to be >= -2147483648"
                >,
                v.MaxValueAction<
                    number,
                    2147483647,
                    "Invalid value: Expected int32 to be <= 2147483647"
                >,
            ]
        >;
        readonly title: v.StringSchema<undefined>;
        readonly type: v.OptionalSchema<
            v.SchemaWithPipe<readonly [v.StringSchema<undefined>, v.UrlAction<string, undefined>]>,
            undefined
        >;
    },
    undefined
>;
export declare const vIssuedRequest: v.ObjectSchema<
    {
        readonly expiresInSeconds: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
        readonly resendAfterSeconds: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
        readonly verificationId: v.StringSchema<undefined>;
    },
    undefined
>;
export declare const vLoginRequest: v.ObjectSchema<
    {
        readonly email: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.EmailAction<string, undefined>,
                v.MinLengthAction<string, 0, undefined>,
                v.MaxLengthAction<string, 254, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
    },
    undefined
>;
export declare const vLoginResponse: v.ObjectSchema<
    {
        readonly accessToken: v.StringSchema<undefined>;
        readonly expiresIn: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
        readonly tokenType: v.PicklistSchema<["Bearer"], undefined>;
    },
    undefined
>;
export declare const vPasswordChangeRequest: v.ObjectSchema<
    {
        readonly accountProof: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.MinLengthAction<string, 1, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
        readonly newPassword: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.MinLengthAction<string, 8, undefined>,
                v.MaxLengthAction<string, 128, undefined>,
            ]
        >;
    },
    undefined
>;
export declare const vSignupRequest: v.ObjectSchema<
    {
        readonly email: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.EmailAction<string, undefined>,
                v.MinLengthAction<string, 1, undefined>,
                v.MaxLengthAction<string, 254, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
        readonly nickname: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.MinLengthAction<string, 1, undefined>,
                v.MaxLengthAction<string, 50, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
    },
    undefined
>;
export declare const vSignupResponse: v.ObjectSchema<
    {
        readonly userId: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
    },
    undefined
>;
export declare const vLoginRequestWritable: v.ObjectSchema<
    {
        readonly email: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.EmailAction<string, undefined>,
                v.MinLengthAction<string, 0, undefined>,
                v.MaxLengthAction<string, 254, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
        readonly password: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.MinLengthAction<string, 8, undefined>,
                v.MaxLengthAction<string, 128, undefined>,
            ]
        >;
    },
    undefined
>;
export declare const vSignupRequestWritable: v.ObjectSchema<
    {
        readonly email: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.EmailAction<string, undefined>,
                v.MinLengthAction<string, 1, undefined>,
                v.MaxLengthAction<string, 254, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
        readonly nickname: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.MinLengthAction<string, 1, undefined>,
                v.MaxLengthAction<string, 50, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
        readonly password: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.MinLengthAction<string, 8, undefined>,
                v.MaxLengthAction<string, 128, undefined>,
            ]
        >;
        readonly signupProof: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.LengthAction<string, 43, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
    },
    undefined
>;
export declare const vDeleteBody: v.ObjectSchema<
    {
        readonly accountProof: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.MinLengthAction<string, 1, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
    },
    undefined
>;
/**
 * 탈퇴 및 계정 정리 완료. Cache-Control: no-store. 본문·새 토큰 없음
 */
export declare const vDeleteResponse: v.VoidSchema<undefined>;
export declare const vRequest1Body: v.ObjectSchema<
    {
        readonly purpose: v.PicklistSchema<["PASSWORD_CHANGE", "ACCOUNT_DELETION"], undefined>;
    },
    undefined
>;
/**
 * SMTP 서버에 발송 요청 완료. 수신함 도착을 보장하지 않음. Cache-Control: no-store
 */
export declare const vRequest1Response: v.ObjectSchema<
    {
        readonly expiresInSeconds: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
        readonly purpose: v.PicklistSchema<["PASSWORD_CHANGE", "ACCOUNT_DELETION"], undefined>;
        readonly resendAfterSeconds: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
        readonly verificationId: v.StringSchema<undefined>;
    },
    undefined
>;
export declare const vConfirm1Body: v.ObjectSchema<
    {
        readonly code: v.SchemaWithPipe<
            readonly [v.StringSchema<undefined>, v.RegexAction<string, undefined>]
        >;
        readonly purpose: v.PicklistSchema<["PASSWORD_CHANGE", "ACCOUNT_DELETION"], undefined>;
        readonly verificationId: v.SchemaWithPipe<
            readonly [v.StringSchema<undefined>, v.UuidAction<string, undefined>]
        >;
    },
    undefined
>;
/**
 * 목적별 일회성 accountProof. Cache-Control: no-store
 */
export declare const vConfirm1Response: v.ObjectSchema<
    {
        readonly accountProof: v.StringSchema<undefined>;
        readonly expiresInSeconds: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
        readonly purpose: v.PicklistSchema<["PASSWORD_CHANGE", "ACCOUNT_DELETION"], undefined>;
    },
    undefined
>;
/**
 * CSRF 쿠키 준비 완료 (Cache-Control: no-store)
 */
export declare const vCsrfResponse: v.VoidSchema<undefined>;
export declare const vRequestBody: v.ObjectSchema<
    {
        readonly email: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.EmailAction<string, undefined>,
                v.MinLengthAction<string, 0, undefined>,
                v.MaxLengthAction<string, 254, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
    },
    undefined
>;
/**
 * SMTP 서버에 발송 요청 완료. 수신함 도착을 보장하지 않음
 */
export declare const vRequestResponse: v.ObjectSchema<
    {
        readonly expiresInSeconds: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
        readonly resendAfterSeconds: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
        readonly verificationId: v.StringSchema<undefined>;
    },
    undefined
>;
export declare const vConfirmBody: v.ObjectSchema<
    {
        readonly code: v.SchemaWithPipe<
            readonly [v.StringSchema<undefined>, v.RegexAction<string, undefined>]
        >;
        readonly email: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.EmailAction<string, undefined>,
                v.MinLengthAction<string, 0, undefined>,
                v.MaxLengthAction<string, 254, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
        readonly verificationId: v.SchemaWithPipe<
            readonly [v.StringSchema<undefined>, v.UuidAction<string, undefined>]
        >;
    },
    undefined
>;
/**
 * 이메일에 바인딩된 가입용 증명 발급. Cache-Control: no-store
 */
export declare const vConfirmResponse: v.ObjectSchema<
    {
        readonly expiresInSeconds: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
        readonly signupProof: v.StringSchema<undefined>;
    },
    undefined
>;
export declare const vLoginBody: v.ObjectSchema<
    {
        readonly email: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.EmailAction<string, undefined>,
                v.MinLengthAction<string, 0, undefined>,
                v.MaxLengthAction<string, 254, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
        readonly password: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.MinLengthAction<string, 8, undefined>,
                v.MaxLengthAction<string, 128, undefined>,
            ]
        >;
    },
    undefined
>;
/**
 * Access Token과 Refresh 쿠키 발급. Cache-Control: no-store
 */
export declare const vLoginResponse2: v.ObjectSchema<
    {
        readonly accessToken: v.StringSchema<undefined>;
        readonly expiresIn: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
        readonly tokenType: v.PicklistSchema<["Bearer"], undefined>;
    },
    undefined
>;
/**
 * 현재 로그인의 후속 갱신 차단 및 Refresh 쿠키 만료. 본문 없음. 쿠키가 없거나 유효하지 않으면 브라우저 정리만 수행. Cache-Control: no-store
 */
export declare const vLogoutResponse: v.VoidSchema<undefined>;
export declare const vChangeBody: v.ObjectSchema<
    {
        readonly accountProof: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.MinLengthAction<string, 1, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
        readonly newPassword: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.MinLengthAction<string, 8, undefined>,
                v.MaxLengthAction<string, 128, undefined>,
            ]
        >;
    },
    undefined
>;
/**
 * 비밀번호 변경과 기존 로그인 무효화 완료. Cache-Control: no-store. 재로그인 필요
 */
export declare const vChangeResponse: v.VoidSchema<undefined>;
/**
 * 새 Access Token과 교체된 Refresh 쿠키. Cache-Control: no-store
 */
export declare const vRefreshResponse: v.ObjectSchema<
    {
        readonly accessToken: v.StringSchema<undefined>;
        readonly expiresIn: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
        readonly tokenType: v.PicklistSchema<["Bearer"], undefined>;
    },
    undefined
>;
export declare const vSignupBody: v.ObjectSchema<
    {
        readonly email: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.EmailAction<string, undefined>,
                v.MinLengthAction<string, 1, undefined>,
                v.MaxLengthAction<string, 254, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
        readonly nickname: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.MinLengthAction<string, 1, undefined>,
                v.MaxLengthAction<string, 50, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
        readonly password: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.MinLengthAction<string, 8, undefined>,
                v.MaxLengthAction<string, 128, undefined>,
            ]
        >;
        readonly signupProof: v.SchemaWithPipe<
            readonly [
                v.StringSchema<undefined>,
                v.LengthAction<string, 43, undefined>,
                v.RegexAction<string, undefined>,
            ]
        >;
    },
    undefined
>;
/**
 * 회원 생성 및 DB 커밋 완료. Cache-Control: no-store
 */
export declare const vSignupResponse2: v.ObjectSchema<
    {
        readonly userId: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
    },
    undefined
>;
export declare const vCreateBody: v.ObjectSchema<
    {
        readonly name: v.NullishSchema<
            v.SchemaWithPipe<
                readonly [v.StringSchema<undefined>, v.MaxLengthAction<string, 20, undefined>]
            >,
            undefined
        >;
    },
    undefined
>;
/**
 * 방과 방장 멤버 생성 완료
 */
export declare const vCreateResponse: v.ObjectSchema<
    {
        readonly roomId: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
    },
    undefined
>;
/**
 * 현재 회원 정보. Cache-Control: no-store
 */
export declare const vMeResponse: v.ObjectSchema<
    {
        readonly email: v.NullableSchema<v.StringSchema<undefined>, undefined>;
        readonly nickname: v.StringSchema<undefined>;
        readonly profileImageUrl: v.NullableSchema<v.StringSchema<undefined>, undefined>;
        readonly role: v.PicklistSchema<["USER", "ADMIN"], undefined>;
        readonly userId: v.SchemaWithPipe<
            readonly [
                v.NumberSchema<undefined>,
                v.IntegerAction<number, undefined>,
                v.MinValueAction<number, -9007199254740991, undefined>,
                v.MaxValueAction<number, 9007199254740991, undefined>,
            ]
        >;
    },
    undefined
>;
