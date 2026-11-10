# 백엔드 작업 규칙

이 지침은 `backend/` 전체의 운영 코드·테스트·설정에 적용합니다. 저장소 공통 작업과 스킬 사용은 [루트 지침](../AGENTS.md)을 따릅니다.
실행·개인 설정은 [README](README.md), DB 변경은 [마이그레이션 안내](gradle/README.md), 기능별 동작과 검증은 [기능 문서](docs/)에서 확인합니다.

## Java 타입 선언

- Java 21을 사용합니다. 운영·테스트 코드 모두 `var`를 사용하지 않습니다. 지역 변수, 향상된 for문, try-with-resources도 실제 타입을 명시합니다. Lombok의 `var`·`val`로 우회하지 않습니다.
- 반환 타입과 제네릭 인자를 확인하고 `Optional<AccountLookup.Credentials>`, `List<Locale.LanguageRange>`, `ResponseEntity.BodyBuilder`처럼 선언만으로 타입을 알 수 있게 작성합니다.
- 타입을 숨기기 위해 `Object`나 raw type으로 바꾸지 않습니다. 외부 API가 실제로 반환하는 `Object`, 와일드카드, 기존 라이브러리의 raw type 제약은 해당 계약에 맞게 유지합니다.
- 중첩 타입은 `AccountVerificationStore.Subject`처럼 소유 타입을 드러내는 표기를 우선합니다. 필요한 import를 추가하고 타입 이름 충돌이 없으면 선언에 전체 패키지 경로를 반복하지 않습니다.
- 타입 선언만 정리하는 작업에서는 초기화식·분기·예외·null 처리·트랜잭션·외부 계약을 함께 바꾸지 않습니다.

## 엔티티 getter

- 필드를 그대로 반환하는 getter는 직접 구현하지 않고 Lombok `@Getter`로 생성합니다.
- `@Getter`는 협력에 필요한 필드에만 붙입니다. 엔티티 클래스 전체에 붙이면 이후 추가되는 필드까지 자동으로 공개되므로 사용하지 않습니다. 엔티티에는 `@Setter`, `@Data`를 사용하지 않습니다.
- 예외로 감사 시각처럼 읽기 전용 메타데이터만 가진 클래스(`BaseTimeEntity`)는 클래스 전체에 `@Getter`를 붙일 수 있습니다.
- 값을 계산하거나 연관 객체에서 꺼내는 메서드(`RoomMember.isOwner`, `RoomInvitation.getRoomId`)는 직접 구현합니다.

## API URL

- 도메인 API(room, chat, users, trip, 이후 매칭 등)는 명사형 리소스 URL만 사용하고 경로에 동사를 넣지 않습니다. 좋은 예 `DELETE /api/rooms/{roomId}/members/me`, 피할 예 `POST /api/rooms/{roomId}/leave`.
- 새로 생기는 것은 컬렉션에 `POST`, 상태 전환은 그 리소스에 `PATCH`와 `{status}`, 제거·탈퇴는 `DELETE`를 사용합니다. 좋은 예 `PATCH /api/applications/{id}` + `{status}`, 피할 예 `POST /api/applications/{id}/approve`.
- 현재 사용자의 것은 `me`로 표현하고 userId를 경로나 본문으로 받지 않습니다. 좋은 예 `PATCH /api/users/me`, 피할 예 `PATCH /api/users/{userId}`.
- Path는 대상 리소스 식별, Query는 조회 필터·정렬·페이지, Body는 생성·수정 데이터에 사용합니다. 초대 코드 같은 비밀값은 Path에 두지 않습니다. 좋은 예 `POST /api/room-members` + `{invitationCode}`, 피할 예 `POST /api/room-invitations/{code}/join`.
- 중첩은 한 단계로 얕게 유지합니다. 컬렉션은 클라이언트가 부모 ID를 알 때만 부모 아래에 두고, 개별 항목은 평평하게 둡니다. 단, `me`처럼 부모 없이는 대상을 특정할 수 없는 항목은 부모 아래에 둡니다(`DELETE /api/rooms/{roomId}/members/me`). 부모 ID를 모르면 평평한 컬렉션과 Body를 사용합니다(`POST /api/room-members` + `{invitationCode}`). 좋은 예 `GET /api/posts/{postId}/applications`와 `GET /api/applications/{id}`, 피할 예 `GET /api/posts/{postId}/applications/{id}`.
- 예외로 `/api/auth/**`의 인증 엔드포인트(logout, refresh, verify 등)는 기존 관용 동사를 허용합니다. 이 예외를 도메인 API로 넓히지 않습니다. 허용 예 `POST /api/auth/refresh`, 피할 예 `POST /api/trips/{tripId}/refresh`.

## 작업 순서

1. 연결 이슈와 변경 파일, 해당 기능 문서, 호출 경로, 기존 테스트를 확인합니다.
2. 변경할 동작과 API·DB·외부 연동 영향을 정리합니다. 큰 기능은 검증 가능한 단계로 나눕니다.
3. 기존 구현을 우선 재사용하고 필요한 코드·설정·문서만 수정합니다. 동작이 바뀌면 해당 동작의 회귀 검증을 보완하며, 선언 변경을 그대로 복제하는 테스트는 추가하지 않습니다.
4. 스키마 변경은 새 Flyway 버전으로 추가합니다. 적용된 마이그레이션을 수정하거나 Hibernate 자동 DDL로 대체하지 않습니다.
5. API 계약이 바뀌면 실행 서버의 명세에서 `frontend/contracts/openapi.json`과 생성 타입을 함께 갱신합니다. [프론트엔드 안내](../frontend/README.md)에 따라 `pnpm api:pull` 후 diff를 검토하고 `pnpm api:check`, `pnpm build`를 실행합니다. 타입 선언만 바꾼 경우에는 계약 파일을 불필요하게 재생성하지 않습니다.
6. 변경 범위에 맞는 검증을 실행하고 최종 diff를 검토합니다. 구현·로컬 검증·커밋·PR·CI·병합 상태를 구분해 보고합니다.

## 검증

아래 명령은 저장소 루트 기준입니다.

```bash
# 중간 단계의 운영·테스트 컴파일
./backend/gradlew -p backend compileJava compileTestJava
# 필요한 동작을 빠르게 확인할 때
./backend/gradlew -p backend test --tests '*대상테스트클래스'
# Java 코드 변경 후 최종 빌드·전체 테스트
./backend/gradlew -p backend build
# 선언 규칙 확인: 검색 결과가 없어야 함 (이때 rg 종료 코드는 1)
rg -n '\bvar\b|lombok\.val|lombok\.var' backend/src --glob '*.java'
git diff --check
```

- 통합 테스트는 Docker 엔진이 필요하며 `DatabaseIntegrationTest`의 임시 MySQL·Redis를 사용합니다. 개인 DB·개발 Compose·실제 SMTP를 테스트 대상으로 사용하지 않습니다. 상세 범위는 [인증 테스트 안내](docs/auth-testing.md)를 따릅니다.
- 일반 `build`는 프론트엔드를 빌드하지 않습니다. 통합 JAR 변경 시에만 README의 `-PincludeFrontend` 빌드를 추가로 확인합니다.
- Swagger 인증 JavaScript 변경 시 `node --test backend/src/test/js/swagger-auth.test.mjs`도 실행합니다.
- 통과한 검사는 관련 변경이나 실패가 없으면 반복하지 않습니다. 실행 환경 때문에 검증하지 못한 항목은 실제 이유와 함께 보고합니다.
