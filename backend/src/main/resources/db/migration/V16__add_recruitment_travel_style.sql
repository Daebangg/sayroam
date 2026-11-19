-- 기존 모집은 실제 선택값을 복원할 수 없으므로 프로필에서 임의로 채우지 않는다.
ALTER TABLE companion_search
    ADD COLUMN style_planning INT NULL,
    ADD COLUMN style_daily_pace INT NULL,
    ADD COLUMN style_time_together INT NULL,
    ADD COLUMN style_spending INT NULL,
    ADD CONSTRAINT chk_companion_search_style CHECK (
        (style_planning IS NULL AND style_daily_pace IS NULL
         AND style_time_together IS NULL AND style_spending IS NULL)
        OR
        (style_planning IS NOT NULL AND style_daily_pace IS NOT NULL
         AND style_time_together IS NOT NULL AND style_spending IS NOT NULL
         AND style_planning BETWEEN 1 AND 5 AND style_daily_pace BETWEEN 1 AND 5
         AND style_time_together BETWEEN 1 AND 5 AND style_spending BETWEEN 1 AND 5)
    );
