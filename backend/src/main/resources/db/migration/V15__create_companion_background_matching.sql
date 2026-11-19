ALTER TABLE companion_search
    ADD COLUMN matching_revision BIGINT NOT NULL DEFAULT 0,
    ADD INDEX idx_companion_search_matching (destination_id, status, end_date, id);

CREATE TABLE companion_matching_job (
    id BIGINT NOT NULL AUTO_INCREMENT,
    search_id BIGINT NOT NULL,
    matching_revision BIGINT NOT NULL,
    candidate_cursor BIGINT NOT NULL DEFAULT 0,
    status ENUM('PENDING', 'PROCESSING', 'RETRY', 'SUCCEEDED', 'SKIPPED') NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    available_at DATETIME(6),
    claim_token VARCHAR(36),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_companion_matching_job_available (available_at, id),
    CONSTRAINT chk_companion_matching_job_available CHECK (
        (status IN ('PENDING', 'PROCESSING', 'RETRY') AND available_at IS NOT NULL)
        OR (status IN ('SUCCEEDED', 'SKIPPED') AND available_at IS NULL)
    ),
    CONSTRAINT fk_companion_matching_job_search FOREIGN KEY (search_id) REFERENCES companion_search (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE companion_discovery (
    id BIGINT NOT NULL AUTO_INCREMENT,
    first_search_id BIGINT NOT NULL,
    second_search_id BIGINT NOT NULL,
    first_revision BIGINT NOT NULL,
    second_revision BIGINT NOT NULL,
    overlap_start DATE NOT NULL,
    overlap_end DATE NOT NULL,
    last_matched_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_companion_discovery_pair UNIQUE (first_search_id, second_search_id),
    CONSTRAINT chk_companion_discovery_order CHECK (first_search_id < second_search_id),
    CONSTRAINT chk_companion_discovery_period CHECK (overlap_start <= overlap_end),
    CONSTRAINT fk_companion_discovery_first FOREIGN KEY (first_search_id) REFERENCES companion_search (id) ON DELETE CASCADE,
    CONSTRAINT fk_companion_discovery_second FOREIGN KEY (second_search_id) REFERENCES companion_search (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 기존 활성 모집도 최초 배포 후 처리한다. 매칭 날짜 기준은 Asia/Seoul이다.
INSERT INTO companion_matching_job
    (search_id, matching_revision, candidate_cursor, status, attempts, available_at, created_at, updated_at)
SELECT c.id, c.matching_revision, 0, 'PENDING', 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM companion_search c
JOIN users u ON u.id = c.user_id AND u.deleted_at IS NULL
JOIN destination d ON d.id = c.destination_id AND d.is_active = TRUE
WHERE c.status = 'ACTIVE' AND c.end_date >= DATE(UTC_TIMESTAMP() + INTERVAL 9 HOUR);
