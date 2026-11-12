ALTER TABLE travel_style_item ADD COLUMN code VARCHAR(50) NULL AFTER id;

-- 기존 질문의 ID와 회원 답변을 보존한다.
UPDATE travel_style_item SET code = CONCAT('legacy-', id) WHERE code IS NULL;

ALTER TABLE travel_style_item
    MODIFY COLUMN code VARCHAR(50) NOT NULL,
    ADD CONSTRAINT uk_travel_style_item_code UNIQUE (code),
    DROP COLUMN name,
    DROP COLUMN min_label,
    DROP COLUMN max_label;

-- 화면 문구는 질문 코드에 대응하는 번역 리소스에서 관리한다.
INSERT INTO travel_style_item (code, sort_order, is_active, created_at, updated_at)
VALUES
    ('planning', 1, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('daily-pace', 2, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('time-together', 3, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('spending', 4, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
