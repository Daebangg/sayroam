package com.daebangg.sayroam.travelstyle.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "user_travel_style",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_travel_style_user_item", columnNames = {"user_id", "item_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTravelStyle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private TravelStyleItem item;

    @Column(nullable = false)
    private int score;

    @Builder
    private UserTravelStyle(Long userId, TravelStyleItem item, int score) {
        this.userId = userId;
        this.item = item;
        this.score = score;
    }

}
