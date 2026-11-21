-- 회원이 지도에서 직접 고른 장소(CUSTOM)를 저장한다.
-- 직접 고른 장소는 원본 ID·분류·원본 분류 코드가 없으므로 해당 열을 NULL로 허용하되,
-- TOUR 장소는 기존처럼 세 값이 모두 있어야 하고 CUSTOM 장소는 원본 ID를 가질 수 없다.
-- 분류는 이후 회원이 직접 고를 수 있도록 CUSTOM에서도 NULL을 강제하지 않는다.
-- uk_place_source는 NULL을 서로 다른 값으로 보므로 CUSTOM 장소는 여러 개 저장된다.
ALTER TABLE place
    MODIFY source ENUM('TOUR', 'CUSTOM') NOT NULL,
    MODIFY source_id VARCHAR(40) NULL,
    MODIFY category ENUM('ATTRACTION', 'CULTURE', 'ACTIVITY', 'LODGING', 'SHOPPING', 'RESTAURANT') NULL,
    MODIFY source_category_code VARCHAR(20) NULL,
    ADD CONSTRAINT ck_place_source_reference CHECK (
        (source = 'TOUR' AND source_id IS NOT NULL AND category IS NOT NULL AND source_category_code IS NOT NULL)
        OR (source = 'CUSTOM' AND source_id IS NULL)
    );
