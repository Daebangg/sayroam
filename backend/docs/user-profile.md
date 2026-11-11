# 내 정보 조회·프로필 부분 수정 (#42)

회원 생성은 [회원가입](signup.md), 개인정보 정리·익명화는 [회원 탈퇴](account-deletion.md)를 재사용합니다.
회원가입 시 성별·출생연도·이미지 URL은 필수가 아닙니다. 프로필 사진 업로드는 아래 전용 API를 사용합니다.

## 요청·응답

`GET /api/users/me`와 `PATCH /api/users/me`는 `Authorization: Bearer <accessToken>`으로 본인만 접근합니다.
회원 ID는 인증 결과에서 가져오며 요청 본문·쿼리의 ID로 조회 대상을 바꾸지 않습니다.
두 API는 Refresh 쿠키·CSRF 헤더를 인증 수단으로 사용하지 않습니다.
성공 시 `200 application/json`, `Cache-Control: no-store`와 같은 응답 구조를 제공합니다.

```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "여행자",
  "profileImageUrl": null,
  "gender": null,
  "birthYear": null,
  "role": "USER",
  "createdAt": "2026-11-10T12:00:00Z",
  "birthDate": null
}
```

모든 응답 필드가 존재하며 `email`·`profileImageUrl`·`gender`·`birthYear`·`birthDate`는 null일 수 있습니다.
`createdAt`은 수정할 수 없는 가입 시각이며 UTC ISO 8601 형식입니다.
`gender`는 `MALE`·`FEMALE`·null입니다. 선택 정보의 null은 미설정 상태를 의미합니다.

PATCH 본문은 JSON 객체이며 아래 다섯 필드만 허용합니다. `{}`는 현재 상태를 그대로 반환합니다.
생략한 필드는 유지하고, 선택 정보의 명시적 null은 기존 값을 제거합니다.

| 필드 | 설정·변경 기준 | null |
| --- | --- | --- |
| `nickname` | 앞뒤 공백 제거 후 필수 1~50자, 제어·숨김·줄바꿈 문자 불가 | 거절 |
| `profileImageUrl` | 앞뒤 공백 제거 후 최대 500자, 호스트가 있는 HTTP(S) URL 또는 제공된 `/brand/avatars/{preset}.svg` 경로, 사용자 정보 불가 | 제거 |
| `gender` | `MALE` 또는 `FEMALE` 문자열 | 미설정으로 변경 |
| `birthDate` | `YYYY-MM-DD` 형식의 실제 날짜, 오늘 이하 | 생일·출생연도 제거 |
| `birthYear` | JSON 정수, 1 이상·현재 연도 이하 | 미설정으로 변경 |

`birthDate`를 저장하면 `birthYear`도 해당 연도로 맞춥니다. 두 필드는 한 요청에 함께 보낼 수 없습니다.
기존 연도만 있는 회원의 생일은 임의로 만들지 않고 null로 유지합니다. 기존 `birthYear` 변경은 정확한 생일과 연도가 다를 때 생일을 지웁니다.

예를 들어 아래 요청은 닉네임만 바꾸고 성별을 지우며, 기존 이미지·출생연도는 유지합니다.

```http
PATCH /api/users/me
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{"nickname": "제주 여행자", "gender": null}
```

숫자를 문자열로 보내거나 소수·범위 초과 정수를 보내면 자동 변환하지 않고 거절합니다.
`userId`·`email`·`role`·`emailVerifiedAt`·`deletedAt`·`authVersion` 등 허용하지 않은 필드도 거절합니다.
하나라도 잘못된 값이면 요청 전체를 롤백합니다. 변경은 회원 행 잠금 안에서 처리해 서로 다른 필드의
동시 PATCH가 기존 값을 덮어쓰지 않도록 합니다. 프로필 변경은 인증 버전을 증가시키지 않습니다.

## 프로필 사진 업로드 (#98)

`POST /api/users/me/profile-image`는 Bearer 인증으로 본인의 사진만 저장합니다.
본문은 `{ "dataUrl": "data:image/jpeg;base64,..." }`이며 JPEG·PNG만 허용합니다.
디코딩한 원본 최대 2MB, 가로·세로 최대 4096px를 검사하고 가운데를 잘라
512×512 JPEG로 다시 인코딩합니다. 원본 메타데이터는 보관하지 않습니다.
프론트엔드는 최대 5MB JPG·PNG 파일을 선택받아 미리보기용 512px JPEG로 줄인 후 전송합니다.
사진 선택 후 원형 범위 조정 모달에서 선택 영역 이동·모서리 크기 조절을 할 수 있고 적용 버튼을 누르면 즉시 업로드합니다.

사진 바이트는 `user_profile_image`에 회원당 하나만 저장하고 `users.profile_image_url`에
`/images/profiles/{UUID}.jpg`를 기록합니다. 새 업로드는 이전 사진을 교체하며, UUID도 변경합니다.
이미지는 로그인 없이 조회할 수 있는 프로필 공개 리소스입니다. GET은 JPEG와 `Cache-Control: no-store`를 반환합니다.
기본 프로필로 변경하거나 탈퇴하면 업로드 사진을 삭제하며, 이전 URL은 404가 됩니다.
다른 회원의 업로드 URL을 PATCH로 지정할 수 없습니다. V8 마이그레이션을 적용해야 합니다.

## 실패·명세

실패는 공통 `application/problem+json` 구조입니다. 400 응답의 `code`로 아래 업무 오류를 구분합니다.

| 코드 | 원인 |
| --- | --- |
| `USER_NICKNAME_REQUIRED` | null·빈 닉네임 |
| `USER_NICKNAME_TOO_LONG` | 최대 길이 초과 |
| `USER_NICKNAME_INVALID` | 제어·숨김·줄바꿈 문자 포함 |
| `USER_PROFILE_IMAGE_URL_TOO_LONG` | 이미지 URL 최대 길이 초과 |
| `USER_PROFILE_IMAGE_URL_INVALID` | 허용하지 않는 URL 형식 |
| `USER_PROFILE_IMAGE_INVALID` | 지원하지 않는 사진 형식·손상·용량·크기 초과 |
| `USER_BIRTH_DATE_INVALID` | 연도가 양수가 아니거나 미래 생일 |
| `USER_BIRTH_YEAR_INVALID` | 0 이하 또는 미래 출생연도 |

잘못된 JSON·필드 타입·성별·보호 필드는 Spring 요청 오류로 400을 반환합니다.
미인증·만료·탈퇴·인증 버전 불일치는 401과 `WWW-Authenticate: Bearer`,
인증 저장소 장애는 503, 처리하지 못한 서버 오류는 500 계약을 따릅니다.

OpenAPI는 `UserApi`·요청/응답 DTO·공통 `OpenApiConfig`에서 생성합니다.
프론트엔드의 저장된 계약·타입·경로표는 서버 재시작 후 `frontend/`에서
`pnpm api:pull --url http://localhost:8080/v3/api-docs`로 갱신합니다.
`api:generate`는 저장된 계약에서 타입만 다시 생성하므로 새 API를 받아오지 않습니다.

`AuthIntegrationTests`에서 가입 → 조회 → 부분 수정 → 재조회 → 탈퇴, null·생략 구분,
입력 오류의 롤백·본인 접근·인증 폐기·동시 PATCH·OpenAPI를 검증합니다.
실제 메일·프론트엔드 브라우저 검증 범위는 [인증 테스트 안내](auth-testing.md)를 참고합니다.
