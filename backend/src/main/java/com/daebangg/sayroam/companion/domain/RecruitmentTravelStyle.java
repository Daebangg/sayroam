package com.daebangg.sayroam.companion.domain;

import com.daebangg.sayroam.companion.domain.error.CompanionSearchErrorCode;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import com.daebangg.sayroam.travelstyle.domain.UserTravelStyle;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;

/** 프로필과 독립적으로 모집에 저장하는 여행 스타일. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentTravelStyle {
    @Column(name = "style_planning")
    private Integer planning;
    @Column(name = "style_daily_pace")
    private Integer dailyPace;
    @Column(name = "style_time_together")
    private Integer timeTogether;
    @Column(name = "style_spending")
    private Integer spending;

    public RecruitmentTravelStyle(Map<String, Integer> answers) {
        if (answers == null || !TravelStyleMatch.QUESTION_CODES.containsAll(answers.keySet())
                || answers.values().stream().anyMatch(score -> score != null
                && (score < UserTravelStyle.MIN_SCORE || score > UserTravelStyle.MAX_SCORE))) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        if (!TravelStyleMatch.isComplete(answers)) {
            throw new BusinessException(CompanionSearchErrorCode.TRAVEL_STYLE_REQUIRED);
        }
        planning = answers.get("planning");
        dailyPace = answers.get("daily-pace");
        timeTogether = answers.get("time-together");
        spending = answers.get("spending");
    }

    public Map<String, Integer> answers() {
        return Map.of("planning", planning, "daily-pace", dailyPace,
                "time-together", timeTogether, "spending", spending);
    }
}
