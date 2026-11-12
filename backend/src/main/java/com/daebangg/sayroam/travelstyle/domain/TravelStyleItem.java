package com.daebangg.sayroam.travelstyle.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 여행 스타일 질문. 질문을 없앨 때 행을 지우면 기존 답변이 깨지므로 active를 false로 바꾼다
@Entity
@Table(name = "travel_style_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelStyleItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    @Getter
    private String code;

    @Column(nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
