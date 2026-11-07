# sayroam

- `backend/`: Spring Boot 백엔드 (Java 21, Gradle)
- `frontend/`: React·TypeScript 프론트엔드 (Vite, pnpm)
- `docker/`: 로컬 MySQL·Redis 개발 환경

- [프론트엔드 설치·실행·검증 방법](frontend/README.md)
- [로컬 앱 환경변수 및 실행 방법](backend/README.md)
- [화면을 포함한 배포용 JAR 빌드](backend/README.md#프론트엔드를-포함한-배포용-빌드)
- [Docker 로컬 개발 환경 및 실행 명령](docker/README.md)

## 변경 범위별 CI

`dev`·`main` 대상 PR과 `dev` push에서 변경 파일에 따라 백엔드·프론트엔드 검증을 각각 실행합니다.

| 변경 범위 | 실행할 CI |
| --- | --- |
| `backend/**` 코드·설정 | Backend CI: `./gradlew build` |
| `frontend/**` 코드·설정·정적 자원 | Frontend CI: 잠금 파일 설치 → `pnpm check` → `pnpm build` |
| 양쪽 코드·설정 | Backend CI와 Frontend CI |
| Markdown(`*.md`), `backend/docs/**`, `frontend/notes/**`만 변경 | 두 CI 모두 생략 |
| 각 CI의 workflow 파일 | 해당 CI |

루트 문서와 `docker/**` 변경은 두 CI의 실행 대상에 포함하지 않습니다.
현재 자동 검증은 백엔드·프론트엔드 두 범위이며, 통합 JAR·Docker·배포·문서 전용 검증은 별도 후속 범위입니다.
