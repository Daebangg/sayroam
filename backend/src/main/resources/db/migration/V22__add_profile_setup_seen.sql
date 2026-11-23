-- 기존 회원은 초기 안내를 생략하고, 이후 가입하는 회원만 처음에 안내한다.
ALTER TABLE users ADD COLUMN profile_setup_seen BIT(1) NOT NULL DEFAULT b'1';
ALTER TABLE users ALTER COLUMN profile_setup_seen SET DEFAULT b'0';
