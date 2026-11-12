package com.daebangg.sayroam.travelstyle.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "user_travel_style",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_travel_style_user_item", columnNames = {"user_id", "item_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTravelStyle extends BaseTimeEntity {
    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private TravelStyleItem item;

    @Column(nullable = false)
    @Getter
    private int score;

    public UserTravelStyle(Long userId, TravelStyleItem item, int score) {
        this.userId = userId;
        this.item = item;
        changeScore(score);
    }

    public Long getItemId() {
        return item.getId();
    }

    public void changeScore(int score) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        this.score = score;
    }
}
