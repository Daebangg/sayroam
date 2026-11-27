.DEFAULT_GOAL := help

# macOS 기본 GNU Make 3.81에서도 실행한다. 경로는 저장소 루트 기준이다.
GRADLE := ./backend/gradlew -p backend
GRADLE_ARGS ?=
APP_ARGS ?= --spring.profiles.active=local
FRONTEND_ARGS ?=

.PHONY: help env-init frontend-install backend-run frontend-run \
	backend-build frontend-build build backend-test frontend-check check \
	docker-up docker-status docker-logs docker-stop docker-down \
	db-info db-migrate db-validate require-java require-pnpm require-docker

help: ## 명령 목록과 용도 표시 (기본 동작)
	@printf 'SayRoam 개발 명령 (저장소 루트에서 실행)\n\n'
	@awk 'BEGIN {FS = ":.*## "} /^[a-z-]+:.*## / {printf "  %-20s %s\n", $$1, $$2}' Makefile
	@printf '\n처음 시작: make env-init → 개인 설정 수정 → make frontend-install\n'
	@printf '이후: make docker-up → make db-migrate → 터미널별 make backend-run / make frontend-run\n'

env-init: ## 없는 개인 설정만 예시에서 복사 (기존 파일 보존)
	@set -e; for destination in docker/.env backend/config/application-local.properties frontend/.env.local; do \
		template="$$destination.example"; \
		if [ "$$destination" = frontend/.env.local ]; then template=frontend/.env.example; fi; \
		if [ -e "$$destination" ] || [ -L "$$destination" ]; then \
			printf '유지: %s\n' "$$destination"; \
		else \
			cp -n "$$template" "$$destination"; \
			printf '생성: %s\n' "$$destination"; \
		fi; \
	done
	@printf 'DB·Redis 접속 정보와 Access Token 서명 키를 개인 설정에 입력하세요. README의 처음 실행 순서를 참고하세요.\n'

# Corepack가 frontend/package.json의 고정 pnpm 버전을 선택하도록 해당 디렉터리에서 호출한다.
frontend-install: require-pnpm ## 잠금 파일 기준 프론트엔드 의존성 설치
	cd frontend && pnpm install --frozen-lockfile

backend-run: require-java ## local 프로필로 백엔드 실행 (DB 기동·마이그레이션은 별도)
	$(GRADLE) bootRun --args='$(APP_ARGS)' $(GRADLE_ARGS)

frontend-run: require-pnpm ## Vite 개발 서버 실행
	cd frontend && pnpm dev $(FRONTEND_ARGS)

backend-build: require-java ## 백엔드 빌드·테스트 (프론트엔드 제외, 테스트용 Docker 필요)
	$(GRADLE) build $(GRADLE_ARGS)

frontend-build: require-pnpm ## 프론트엔드 빌드 (frontend/dist)
	cd frontend && pnpm build

build: require-java require-pnpm ## 테스트와 프론트엔드를 포함한 배포용 JAR 빌드 (Docker 필요)
	$(GRADLE) clean build -PincludeFrontend $(GRADLE_ARGS)

backend-test: require-java ## 임시 DB·Redis로 백엔드 테스트 (Docker 필요)
	$(GRADLE) test $(GRADLE_ARGS)

frontend-check: require-pnpm ## 프론트엔드 타입·린트·회귀·포맷 검증
	cd frontend && pnpm check

check: backend-test frontend-check ## 백엔드 테스트와 프론트엔드 검증

docker-up: require-java require-docker ## 로컬 MySQL·Redis 기동 및 healthy 대기
	$(GRADLE) dockerInit $(GRADLE_ARGS)

docker-status: require-java require-docker ## 로컬 컨테이너 상태 조회
	$(GRADLE) dockerStatus $(GRADLE_ARGS)

docker-logs: require-java require-docker ## MySQL·Redis 최근 로그 조회
	$(GRADLE) dockerLogs $(GRADLE_ARGS)

docker-stop: require-java require-docker ## 로컬 컨테이너 중지
	$(GRADLE) dockerStop $(GRADLE_ARGS)

docker-down: require-java require-docker ## 로컬 컨테이너·네트워크 제거 (MySQL 볼륨 보존)
	$(GRADLE) dockerDown $(GRADLE_ARGS)

db-info: require-java ## DB 마이그레이션 적용·대기 상태 조회
	$(GRADLE) flywayInfo $(GRADLE_ARGS)

db-migrate: require-java ## 미적용 DB 마이그레이션을 명시적으로 적용
	$(GRADLE) flywayMigrate $(GRADLE_ARGS)

db-validate: require-java ## DB 마이그레이션 이력·체크섬 검증
	$(GRADLE) flywayValidate $(GRADLE_ARGS)

require-java:
	@java -version >/dev/null 2>&1 || { printf '%s\n' 'Java 실행 불가: JDK 21을 설치하고 JAVA_HOME·PATH를 확인하세요.' >&2; exit 1; }

require-pnpm:
	@command -v pnpm >/dev/null 2>&1 || { printf '%s\n' 'pnpm 없음: frontend/README.md의 고정 Node.js·pnpm 설치 방법을 따르세요.' >&2; exit 1; }

require-docker:
	@command -v docker >/dev/null 2>&1 || { printf '%s\n' 'Docker 없음: Docker와 Compose 플러그인을 설치하세요.' >&2; exit 1; }
	@docker compose version >/dev/null 2>&1 || { printf '%s\n' 'Docker Compose 실행 불가: Compose 플러그인을 확인하세요.' >&2; exit 1; }
	@test -f docker/.env || { printf '%s\n' 'docker/.env 없음: make env-init 후 접속 정보를 설정하세요.' >&2; exit 1; }
