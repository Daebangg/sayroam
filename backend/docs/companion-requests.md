# 롬 메이트 요청

본인 모집의 후보 카드와 상세는 같은 요청 API를 사용한다. 요청의 기준은 두 모집글의 조합이다. 같은 회원이 올린 다른 모집에는 별도로 요청할 수 있다. 기존 후보 발견 이력과 알림은 [백그라운드 매칭](companion-background-matching.md)에서 다룬다.

| 동작 | API | 결과 |
| --- | --- | --- |
| 요청 보내기 | `POST /api/companion-searches/{searchId}/requests` + `{"candidateSearchId":205}` | 최신 요청을 200으로 반환한다. 양방향 동시 요청의 두 번째 호출에는 최초 요청 기준으로 RECEIVED가 반환된다. |
| 내 모집의 요청 | `GET /api/companion-searches/{searchId}/requests` | 양쪽 방향의 요청을 최신순으로 반환한다. `?status=REJECTED`는 거절된 매칭 목록이다. |
| 알림에서 상세 열기 | `GET /api/companion-requests/{requestId}` | 내 모집과 상대 모집 ID, 나를 기준으로 한 방향, 최신 상태, 상대의 일정·공개 프로필·여행 스타일 비교를 반환한다. 상대가 탈퇴하면 candidate는 null이다. |
| 받은 요청에 응답 | `PATCH /api/companion-requests/{requestId}` + `{"status":"ACCEPTED"}` 또는 `{"status":"REJECTED"}` | 같은 응답을 반복해도 같은 결과를 반환한다. 결정을 뒤집으면 409, 발신자가 응답하면 403, 제삼자가 조회·응답하면 404다. |

후보 카드의 `canRequest`가 true이면 **롬 메이트 요청하기**를 표시한다. 대기 중이면 `requestStatus=PENDING`이며 SENT는 **응답 기다리는 중**, RECEIVED는 **받은 요청**이다. 대표 후보가 바뀌어도 요청 목록과 알림은 원래 모집 조합으로 연결된다. 거절된 조합은 후보의 회원별 순위와 페이지를 계산하기 전에 제외한다. 기간이 지난 기존 모집의 겹치는 일정은 조회할 수 있고 새 요청은 보낼 수 없다.

모집 종료·수정 시 유효성을 잃은 대기 요청을 만료시킨다. 요청 상세·목록은 읽기 전용이며 현재 조건으로 `EXPIRED`를 계산해 반환하고 DB 상태를 변경하지 않는다. 상태 필터도 계산된 결과에 적용한다. 응답·재발신 시 유효성을 다시 검증해 만료 상태를 저장한다. 저장된 만료 상태는 조건을 복구해도 되돌리지 않는다. 만료 후 수락을 시도하면 EXPIRED가 반환되며, 종료·거절·만료된 요청을 다시 보내도 새 요청이 만들어지지 않는다.

최초 수락 시 요청은 `ACCEPTED`, 양쪽 모집은 `CONFIRMED`로 변경하고, 발신자와 수신자 두 명이 참여한 `COMPANION` 채팅방을 만들어 요청의 `roomId`에 연결한다. 두 모집에 남은 다른 대기 요청은 `EXPIRED`로 변경한다. 수락·모집 확정·방 생성·두 사람 참여·대기 요청 만료·알림은 같은 트랜잭션에서 저장하며, 같은 수락을 반복해도 방이나 알림을 다시 만들지 않는다. 회원 ID, 모집 ID, 관련 대기 요청 ID 순으로 잠가 동시 수락과 모집 변경을 직렬화한다.

일반 종료는 `CLOSED`, 동행 확정 종료는 `CONFIRMED`로 구분한다. 확정된 모집은 후보 탐색·노출, 추가 요청 발신·수신, 조건 수정과 종료·재개가 불가능하다. `CONFIRMED`는 일반 모집 상태 변경 API에서 직접 지정할 수 없다. 요청 생성·조회·응답·목록 응답은 nullable `roomId`를 제공하므로 양쪽 회원이 다시 접속해도 같은 방을 찾을 수 있다. 수락 전 요청과 채팅 연결 전의 과거 요청은 `roomId=null`이며, 과거 수락 건에 채팅방을 소급 생성하지 않는다. 수락 파기와 이후 모집 재개·채팅 보존 정책은 후속 작업이다.

요청 생성과 응답은 알림과 같은 트랜잭션에서 저장한다. 수신자는 COMPANION_REQUEST를, 최초 발신자는 COMPANION_ACCEPTED 또는 COMPANION_REJECTED를 받는다. targetType은 COMPANION_REQUEST이며 targetId는 요청 ID다. 사건 키는 요청 ID와 사건 종류로 고정해 같은 사건을 다시 처리해도 한 건만 전달한다.

V25는 기존 수신 모집에 발신 모집을 추가하고 두 ID의 최솟값·최댓값에 유일 제약을 둔다. 발신 모집이 기록되지 않은 과거 행과 기존 방 연결은 보존하며, 해당 행은 두 모집 기반 API에서 제외한다. 새 알림 유형을 처리하고 요청 상세로 연결하는 화면은 [#186](https://github.com/Daebangg/sayroam/issues/186)에서 배포한다. 기존 프론트는 새 알림 유형을 거부하므로 호환 화면을 먼저 또는 서버와 함께 배포한다.

## 서비스와 트랜잭션

모집·후보 탐색·요청 API의 HTTP 진입점은 `CompanionController`, Swagger 선언은 `CompanionApi`로 모은다.
URL·operationId·응답 형식과 모집·요청별 문서 태그는 유지한다.
후보 조회와 미리보기의 Query 파라미터는 각각 `FindCompanionCandidatesRequest`,
`PreviewCompanionCandidatesRequest`로 받고 `toQuery()`로 application의 Query DTO에 전달한다.
페이지·정렬 조건도 Query에 포함하며 생략하거나 빈 값이면 기존 기본값인 0페이지·20건·OVERLAP을 사용한다.
컨트롤러는 `ResponseEntity`를 반환하고 모집 목록 변환은 `CompanionSearchListResponse.from`에 둔다.
사용자 조건 조회는 Trip과 같이 `findAccessible[ForUpdate](userId, 대상 ID)`,
ID 잠금 조회는 `findByIdForUpdate`, 모집 쌍의 요청 잠금 조회는 `findBySearchPairForUpdate`로 표현한다.

모집·요청 유스케이스는 `CompanionService`가 담당한다. 엔티티 조회·저장·잠금은 각 도메인 Repository,
후보·공개 프로필·여행 스타일 조합 조회는 `CompanionQueryRepository`를 사용한다.
채팅·알림 생성은 해당 서비스에 맡기고, 회원 잠금은 호출자의 트랜잭션에 참여하는 `UserService`에서 ID 순으로 처리한다.
여행지 이용 가능 여부는 `DestinationService.isAvailable`로 확인한다. 모집·요청·백그라운드 매칭의 기존 검증 시점과 오류·작업 건너뛰기 처리는 유지한다.
신규 요청은 `CompanionSearch.requestTo`를 통해 생성하며, 내부의 private 검증에서 소유권과 양쪽 모집의 상태·기간·여행지를 확인한다.
서비스가 직접 요청을 생성하거나 모집을 확정하는 경로는 package-private으로 제한한다. 기존 요청 조회는 소유자로 제한한 잠금 조회 후 처리하므로 확정·만료 이후에도 같은 결과를 반환한다.
수신자 확인·만료 반영·응답 상태 변경과 수락 시 양쪽 모집 확정은 `CompanionRequest.respond`에 묶는다.
채팅방 생성·참여·다른 대기 요청 만료·알림은 기존 서비스 트랜잭션에서 이어서 처리한다.
현재 매칭 가능 상태는 `canMatch(today)`,
백그라운드 작업의 조건 버전까지 확인할 때는 `canMatch(revision, today)`를 사용한다.
요청의 양쪽 모집 상태는 `CompanionRequest`가 판단하고, 사용자 프로필을 포함한 조합 조건은
`CompanionCandidateMatchQuery`에 회원·여행지·기간·희망 조건·기준일·상대 모집 ID를 담아
Query Repository의 `isMatchingCandidate`로 확인한다. 요청 발송·응답·백그라운드 탐색에서 같은 조회를 사용한다.
application DTO의 `from`에서 모집 또는 요청을 불변 조회 조건으로 변환하고,
Query Repository에는 엔티티나 지연 로딩 연관을 전달하지 않는다. 서비스는 요청의 발신·수신 모집 연관을 직접 꺼내 판단하지 않는다.
제목 80자·소개글 200자 제한은 기존처럼 `CompanionSearch`에서 Unicode 코드 포인트 기준으로 검증하고,
여행 스타일 점수 범위는 `UserTravelStyle`의 상수를 모집 값 객체와 비교 로직이 공유한다.

| 작업 | 격리 수준 | 이유 |
| --- | --- | --- |
| 모집 생성 | DB 기본값 | 회원 잠금 이후 첫 일반 조회로 활성 모집 수를 확인한다. |
| 모집 수정·상태 변경, 요청 발신·응답 | `READ_COMMITTED` 유지 | 잠금 대기 전 조회가 있거나 여러 요청을 재검증할 때, 각 일반 조회에서 최신 커밋된 조건을 확인한다. |
| 요청 상세·목록 및 모집·후보 조회 | DB 기본값, `readOnly = true` | 조회는 표시 상태만 계산하고 쓰기 잠금과 상태 저장을 수행하지 않는다. |
| 백그라운드 매칭 처리 | `READ_COMMITTED` 유지 | 배치 조회 후 회원·모집 잠금을 얻고 최신 조건을 재검증한다. |

MySQL `REPEATABLE_READ`의 일반 조회는 첫 스냅샷을 재사용하고 `READ_COMMITTED`는 조회마다 새 스냅샷을 사용한다.
격리 수준만으로 처리 순서가 보장되지는 않으므로 회원 ID → 모집 ID → 요청 ID 순서를 함께 지킨다.
격리 동작은 [MySQL 일관 읽기 문서](https://dev.mysql.com/doc/refman/8.4/en/innodb-consistent-read.html)를 따른다.
