# 인증 테스트 실행

Docker 엔진과 Java 21만 준비하면 됩니다. 개발용 `dockerInit`, 개인 설정 파일, SMTP 계정은 필요하지 않습니다.

```bash
# 저장소 루트
./backend/gradlew -p backend build
# 인증 통합 테스트만 실행
./backend/gradlew -p backend test --tests '*AuthIntegrationTests'
# 글로벌 오류 응답·인증 쿠키/헤더 계약만 검증 (Docker 불필요)
./backend/gradlew -p backend test --tests '*GlobalExceptionHandlerTests' --tests '*ApiProblemsTest'
# Swagger 응답 처리 테스트 (Node.js 20 이상, 추가 패키지 없음)
node --test backend/src/test/js/swagger-auth.test.mjs
```

`DatabaseIntegrationTest`는 JVM마다 MySQL 8.4·Redis 7.4 컨테이너를 시작하고 임의의 호스트 포트를 연결합니다.
기존 개발 Compose 컨테이너·볼륨은 사용하지 않습니다. 빈 DB에 현재 버전까지의 마이그레이션을 명시적으로 적용한 뒤 Hibernate가
매핑을 검증합니다. 컨테이너는 테스트 클래스 간 공유하고 JVM 종료 시 Testcontainers Ryuk이 정리합니다.
공유 컨테이너에 클래스별 `@Container` 종료 처리를 섞지 않습니다.

`test` 프로필과 동적 연결 설정으로 개발용 DB·Redis 접속 정보 및 JWT 키를 사용하지 않습니다.
SMTP는 끄고 테스트 전용 메일 수집기로 대체해 `@example.test` 주소의 코드를 확인합니다.
Redis 초기화 명령은 테스트 컨테이너 내부의 `redis-cli`로만 실행합니다.
DB 초기화는 그 컨테이너에 연결된 JdbcTemplate으로 테스트 테이블을 FK 순서대로 비웁니다.
JUnit 병렬 클래스 실행은 사용하지 않습니다.

탈퇴 롤백 검증은 테스트 DB 트리거로 마지막 회원 UPDATE를 실패시킵니다. 트리거 생성 허용 설정도
이 MySQL 컨테이너에만 적용합니다. Redis 장애 검증은 이 Redis 컨테이너만 일시 정지하고 `finally`에서 재개합니다.
명령 timeout은 테스트에서 500ms이며 운영 기본값은 유지합니다.

## 검증 범위

가입부터 비밀번호 재설정·변경·탈퇴·재가입을 이어서 확인하는 검사와 실제 서버·메일·브라우저
검증 범위는 [인증 전체 흐름 검증](auth-user-flows.md)을 참고하세요.

`AuthIntegrationTests`는 인증을 모킹하지 않고 실제 Security 필터, HTTP 컨트롤러, Application 포트,
JPA 트랜잭션과 Redis Lua를 통과합니다. Security MVC 슬라이스는 별도로 접근 정책과 세션 미사용을 확인합니다.
슬라이스에서도 `csrf()` 후처리기 대신 실제 CSRF 쿠키·헤더를 사용합니다.

| 검증한 흐름 | 확인 내용 |
| --- | --- |
| 이메일 인증·가입·로그인·내 정보 | 코드 확인, 가입 증명 일회성 소비, 이메일/닉네임 정규화, 실제 비밀번호 해시, Access 본문·Refresh HttpOnly 쿠키 분리 |
| 프로필 부분 수정 | 가입 시 선택 정보 null, 설정·변경·개별 삭제, 생략 필드 유지, 잘못된 값의 전체 롤백, 보호 필드·다른 회원 변경 차단, 동시 PATCH 보존 |
| 프로필 계약·인증 폐기 | GET/PATCH JSON·Bearer·nullable enum·오류 코드 명세, 비밀번호 변경·탈퇴 후 기존 토큰의 조회·수정 차단 |
| 로그인 거절 | 없는 계정·잘못된 비밀번호·비밀번호 없는 계정의 401 |
| JWT 검증 | 잘못된 서명·issuer·audience·만료·미래 발급·유형·용도·인증 버전의 401 |
| CSRF·Swagger | 쿠키/헤더 누락·변조의 403, Swagger Referer로 우회 불가, 페이지 쿠키 발급·자동 헤더 스크립트·보안 명세 |
| Refresh | 실제 교체, 알려진 ID의 다른 비밀값 거절, 이전 값 재사용 시 로그인 폐기, 동시 갱신 한 건 성공 후 폐기 |
| 로그아웃 | 반복 204, 쿠키 만료, 현재 로그인만 갱신 차단, 다른 로그인 격리, 기존 Access 잔여 시간 허용 |
| 비밀번호 변경 | 목적·회원이 다른 증명 거절, 입력 오류 후 증명 재사용 가능, 정상 변경 후 모든 기존 Access/Refresh 차단, 새 비밀번호 로그인 |
| 비밀번호 찾기·재설정 (#75) | 비로그인 이메일 확인·전용 증명·새 비밀번호 저장, 계정 종류별 동일 발송 경로, 소셜 전용·미가입·탈퇴 차단, CSRF·증명 혼용·만료·재발송·제한·동시 요청, DB 롤백·기존 로그인 무효화 |
| 회원 탈퇴 | 프로필 익명화, 로그인 수단·개인 성향·수신 알림 삭제, 기존 방 멤버십·채팅 보존, 기존 인증 차단, 같은 이메일의 새 ID 가입 |
| 탈퇴 DB 실패 | 삭제 어댑터 전체 롤백, 회원 상태·인증 버전 보존, Redis에서 소비한 증명 복원 없음 |
| 실패·동시 요청 | 코드 오입력 한도, SMTP 포트 실패 변환, 동일 증명의 동시 가입 한 건 성공 |
| Redis timeout | 로그인·갱신·로그아웃의 503, 실패 시 Refresh 쿠키 유지, DB 기반 Access 인증 유지 |
| 글로벌 인증 오류 처리 | 기존 인증 오류 29종의 상태·코드·공개 문구, Refresh 쿠키 만료/유지, Bearer 안내, 재시도 시간 올림, 원인·suppressed 비공개 |
| 공통 실패 종류 | 입력·인증·권한·충돌·만료·요청 제한·일시 장애·외부 처리 실패·내부 오류의 HTTP 상태 변환 |
| 스키마 | 빈 DB의 현재 마이그레이션 적용, 재적용 없음, 엔티티 매핑·감사 시각·UNIQUE/FK 제약 |

전체 Java 테스트 개수와 성공·실패·스킵 결과는 실행 후 `backend/build/reports/tests/test/index.html`에서
확인합니다. Swagger JavaScript 검사는 위의 Node 명령으로 별도 실행합니다.

실제 SMTP 발송·수신과 동일 출처 프론트엔드의 쿠키 연동은 위 전체 흐름 문서의 확인 범위를 따릅니다.
전체 만료·재발급·발송 제한 조합, 다른 출처의 CORS 연동,
비밀번호 변경/탈퇴의 동시 경쟁, DB 접속 단절·커밋 응답 유실·Redis 재연결 후 명령 재전송은 추가 검증 대상입니다.
MockMvc 통합 테스트 통과가 이 항목들의 완료를 뜻하지는 않습니다.

## 로컬 Swagger 자동 인증

`local` 프로필에서는 로그인·갱신 성공 응답의 Access Token을 Swagger의 `bearerAuth`에 자동 등록합니다.
로그아웃·비밀번호 변경·재설정·탈퇴 성공 시 등록된 토큰을 비우며, 계정 변경의 DB 결과 불확실 응답에서도
서버의 쿠키 만료 정책에 맞춰 정리합니다. 다른 출처·다른 API·실패한 로그인 응답의 값은 등록하지 않습니다.
CSRF 헤더는 기존 Springdoc 처리로 자동 전달하고 Refresh 쿠키는 브라우저가 관리합니다.
브라우저 저장소에 토큰을 쓰지 않으며, 페이지를 새로고침하면 다시 로그인하거나 Refresh API를 실행합니다.

`SwaggerAuthIndexTransformer`는 Springdoc 기본 초기화 코드에 응답 처리만 추가합니다.
통합 테스트는 실제로 제공되는 초기화 코드에 CSRF와 응답 처리가 함께 포함되는지 확인하고,
JavaScript 테스트 8개는 토큰 등록·교체·정리, 실패·외부 출처 응답 무시와 저장소 미사용을 검증합니다.
