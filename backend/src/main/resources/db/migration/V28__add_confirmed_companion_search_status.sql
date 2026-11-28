-- 동행 확정 종료를 일반 모집 종료와 구분한다. 기존 요청·모집의 상태는 소급 변경하지 않는다.
ALTER TABLE companion_search
    MODIFY COLUMN status ENUM('ACTIVE', 'CLOSED', 'CONFIRMED') NOT NULL;
