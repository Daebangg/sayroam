-- 장소 출처를 카카오 로컬 API 캐시에서 한국관광공사 TourAPI로 바꾼다.
-- 카카오 응답은 저장할 수 없어, 회원이 여행에 담은 TourAPI 장소만 출처와 원본 ID로 한 번 저장한다.
-- place와 trip_place에 행을 쓰는 기능이 아직 없어 기존 place 행이 없다고 보고 테이블을 다시 만든다.
ALTER TABLE trip_place
    DROP FOREIGN KEY fk_trip_place_place;

DROP TABLE place;

CREATE TABLE place (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source ENUM('TOUR') NOT NULL,
    source_id VARCHAR(40) NOT NULL,
    name VARCHAR(100) NOT NULL,
    category ENUM('ATTRACTION', 'CULTURE', 'ACTIVITY', 'LODGING', 'SHOPPING', 'RESTAURANT') NOT NULL,
    source_category_code VARCHAR(20) NOT NULL,
    address VARCHAR(255),
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_place_source UNIQUE (source, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE trip_place
    ADD CONSTRAINT fk_trip_place_place FOREIGN KEY (place_id) REFERENCES place (id);
