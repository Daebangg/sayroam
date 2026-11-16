-- 기존 자유 입력 지역은 보존하고 신규 등록은 기준 여행지 ID를 사용한다.
ALTER TABLE companion_search
    MODIFY COLUMN region VARCHAR(50) NULL,
    ADD COLUMN destination_id BIGINT NULL AFTER user_id,
    ADD CONSTRAINT fk_companion_search_destination FOREIGN KEY (destination_id) REFERENCES destination (id),
    ADD CONSTRAINT chk_companion_search_destination CHECK ((destination_id IS NULL) <> (region IS NULL));
