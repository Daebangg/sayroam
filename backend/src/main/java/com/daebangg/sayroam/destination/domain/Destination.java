package com.daebangg.sayroam.destination.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Destination extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    @Getter
    private String name;

    @Column(nullable = false, length = 80)
    private String searchName;

    @Column(unique = true, length = 10)
    private String legalRegionCode;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    // 지도에 여행지 전체를 맞춰 보여 줄 위경도 사각형. 먼 섬은 뺀 영역이다(V23 참고)
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal boundsSouth;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal boundsWest;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal boundsNorth;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal boundsEast;
}
