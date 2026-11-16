package com.daebangg.sayroam.place.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원이 여행에 담은 외부 장소. 출처(source)와 원본 ID(sourceId)로 같은 장소를 한 번만 저장하고,
// 원본 분류 코드를 남겨 서비스 분류를 바꿀 때 다시 분류할 수 있게 한다.
@Entity
@Table(
        name = "place",
        uniqueConstraints = @UniqueConstraint(name = "uk_place_source", columnNames = {"source", "source_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseTimeEntity {
    public static final int MAX_SOURCE_ID_LENGTH = 40;
    public static final int MAX_SOURCE_CATEGORY_CODE_LENGTH = 20;
    public static final int MAX_ADDRESS_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlaceSource source;

    @Column(name = "source_id", nullable = false, length = MAX_SOURCE_ID_LENGTH)
    private String sourceId;

    @Embedded
    private PlaceName name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlaceCategory category;

    @Column(name = "source_category_code", nullable = false, length = MAX_SOURCE_CATEGORY_CODE_LENGTH)
    private String sourceCategoryCode;

    // 출처에 주소가 없으면 NULL
    @Column(length = MAX_ADDRESS_LENGTH)
    private String address;

    @Embedded
    private Coordinate coordinate;

    public Place(PlaceSource source, String sourceId, PlaceName name, PlaceCategory category,
                 String sourceCategoryCode, String address, Coordinate coordinate) {
        if (source == null || name == null || category == null || coordinate == null) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        requireText(sourceId, MAX_SOURCE_ID_LENGTH);
        requireText(sourceCategoryCode, MAX_SOURCE_CATEGORY_CODE_LENGTH);
        if (address != null && address.length() > MAX_ADDRESS_LENGTH) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        this.source = source;
        this.sourceId = sourceId;
        this.name = name;
        this.category = category;
        this.sourceCategoryCode = sourceCategoryCode;
        this.address = address;
        this.coordinate = coordinate;
    }

    private static void requireText(String value, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
    }

}
