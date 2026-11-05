-- 비밀번호 변경과 모든 기존 로그인 무효화를 같은 DB 트랜잭션에 포함한다.
ALTER TABLE users
    ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_users_auth_version CHECK (auth_version >= 0);
