# 데이터베이스 마이그레이션

스키마 변경은 Flyway Gradle 명령으로만 실행합니다. 앱은 자동 마이그레이션을 하지 않고
`ddl-auto=validate`로 엔티티 매핑을 검증합니다. 빈 DB에서는 먼저 마이그레이션을 실행해야 앱이 시작됩니다.

## 처음 실행

저장소 루트 기준입니다. 먼저 Docker와 앱의 개인 설정 파일을 준비합니다.
설정 방법은 [앱 개발 환경](../README.md)과 [Docker 환경](../../docker/README.md)을 참고하세요.

```bash
# MySQL·Redis를 기동한다. 기존 MySQL 볼륨과 데이터는 유지한다.
./backend/gradlew -p backend dockerInit

# 적용된 버전과 미적용 버전 확인
./backend/gradlew -p backend flywayInfo

# 미적용 마이그레이션을 순서대로 반영
./backend/gradlew -p backend flywayMigrate

# 적용 이력과 SQL 파일의 체크섬 검증
./backend/gradlew -p backend flywayValidate

# 준비된 DB를 사용해 앱 실행
./backend/gradlew -p backend bootRun --args='--spring.profiles.active=local'
```

`dockerInit`은 MySQL 프로세스·DB·계정과 Redis를 준비합니다. Docker 초기 SQL로 테이블을 만들지 않습니다.
기존 볼륨에서는 초기 SQL이 다시 실행되지 않으므로 버전 갱신은 `flywayMigrate`가 담당합니다.
명령을 재실행하면 미적용 버전만 실행하며, 이미 최신 버전이면 변경하지 않습니다.

## 연결 설정

Gradle 명령은 `backend/config/application-local.properties`의 다음 세 키를 읽습니다.
앱을 `local` 프로필로 실행할 때와 같은 접속 정보입니다.

| 설정 키 | 우선 적용되는 환경변수 |
| --- | --- |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` |

셸 환경변수가 파일보다 우선합니다. 파일 없이 실행하려면 환경변수 세 개를 모두 지정합니다.
명령 시작 시 호스트·포트·DB명을 표시합니다. 비밀번호와 URL 쿼리는 별도로 출력하지 않습니다.
실제 접속값은 커밋하지 않으며, Docker의 `.env`를 앱이나 Flyway가 읽지는 않습니다.

이 Gradle 연동은 위 세 키를 직접 읽습니다. Spring의 전체 프로필·설정 로더를 실행하지 않으므로
`${...}` 참조, `spring.config.import`, 별도 프로필 파일은 지원하지 않습니다.
파일에는 직접 값을 적고 환경별 재정의는 `SPRING_DATASOURCE_*`로 통일합니다.
`-Dflyway.url` 등 별도의 Flyway 연결 설정은 사용하지 않습니다.
IntelliJ Run Configuration에만 지정한 환경변수는 터미널에 전달되지 않습니다.
운영에서 실행할 때는 세 환경변수를 모두 명시해 로컬 설정과 섞이지 않게 합니다.

## Alembic과 비교

| Alembic | Flyway 명령 기반 구성 |
| --- | --- |
| `alembic current`, `history` | `flywayInfo`로 적용·대기 상태 조회 |
| `alembic upgrade head` | `flywayMigrate` |
| `alembic revision -m ...` | 다음 버전 SQL 파일 직접 추가 |
| `revision --autogenerate` | 이 구성에는 자동 변경 감지 없음. 엔티티 변경과 SQL을 함께 작성 |
| `alembic_version` | `flyway_schema_history` |
| `alembic downgrade` | 일반 되돌리기 명령을 제공하지 않음. 후속 변경 SQL로 복구 |

`flywayValidate`는 적용 이력과 파일의 일관성을 검사합니다. DB의 모든 FK·인덱스나 수동 변경을
검증하는 기능은 아닙니다. 앱의 Hibernate validate와 주요 DB 제약 테스트를 함께 사용합니다.

## 변경 파일 규칙

위치는 `src/main/resources/db/migration/`입니다.

```text
V1__create_initial_schema.sql
V2__add_example_column.sql
```

`V2`는 파일명 예시이며 아직 존재하지 않습니다.
현재 V1은 업무 테이블 17개를 생성하며 Flyway는 이력 테이블을 별도로 만듭니다.
샘플 데이터는 포함하지 않습니다. enum은 현재 Hibernate/MySQL 매핑에 맞춘 MySQL ENUM입니다.

- 공유 환경에 적용된 SQL은 수정하지 않고 다음 버전을 추가합니다.
- PR 머지 전 다른 작업과 버전 번호가 겹치는지 확인합니다.
- 엔티티와 SQL은 같은 변경에 포함합니다. FK·UNIQUE·NULL 허용 여부도 함께 검토합니다.
- 기존 DB를 암묵적으로 이력에 등록하지 않습니다(`baselineOnMigrate=false`).
- 스키마 삭제 명령은 비활성화되어 있습니다(`cleanDisabled=true`).
- 최초 V1 이후의 변경도 빈 DB에서 전체 적용과 이전 버전에서 갱신을 확인합니다.

MySQL DDL은 실패 시 전체가 롤백되지 않을 수 있습니다. 실패하면 재실행 전에 적용 이력과
실제 생성된 테이블을 확인하고 실패 원인을 해결해야 합니다. `repair`는 테이블을 되돌리는 명령이
아니므로 오류를 숨기려고 실행하지 않습니다. 데이터가 있는 DB는 복구 SQL을 먼저 검토합니다.

## 검증

```bash
./backend/gradlew -p backend test
```

테스트는 Testcontainers의 임시 MySQL 8.4에서 명시적으로 마이그레이션한 다음 앱 매핑을 검증합니다.
Docker가 필요하며 개인 개발 DB는 사용하지 않습니다. 최초 적용, 재실행, 업무 테이블 수,
JPA 저장·조회, Auditing, nullable 여행 기간, 주요 UNIQUE·FK 제약을 검사합니다.

Flyway 플러그인과 MySQL 모듈은 Spring Boot 4.1.2의 관리 버전인 12.4.0에 맞춰져 있습니다.
Spring Boot를 올릴 때 두 버전을 함께 확인합니다.
