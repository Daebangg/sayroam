package com.daebangg.sayroam.travelstyle.application.service;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import com.daebangg.sayroam.travelstyle.application.dto.TravelStyleItemResult;
import com.daebangg.sayroam.travelstyle.application.dto.UpdateTravelStyleCommand;
import com.daebangg.sayroam.travelstyle.domain.TravelStyleItem;
import com.daebangg.sayroam.travelstyle.domain.TravelStyleItemRepository;
import com.daebangg.sayroam.travelstyle.domain.UserTravelStyle;
import com.daebangg.sayroam.travelstyle.domain.UserTravelStyleRepository;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TravelStyleService {
    private final UserRepository users;
    private final TravelStyleItemRepository items;
    private final UserTravelStyleRepository answers;

    @Transactional(readOnly = true)
    public List<TravelStyleItemResult> findMine(long userId, long authVersion) {
        requireCurrentUser(users.findByIdAndDeletedAtIsNull(userId), authVersion);
        return toResults(items.findAllByActiveTrueOrderBySortOrderAscIdAsc(), findAnswers(userId));
    }

    @Transactional
    public List<TravelStyleItemResult> updateMine(long userId, long authVersion, UpdateTravelStyleCommand command) {
        // 최초 저장·수정과 회원 탈퇴가 같은 회원 행을 잠가 순서대로 처리된다.
        requireCurrentUser(users.findForUpdateByIdAndDeletedAtIsNull(userId), authVersion);
        List<TravelStyleItem> activeItems = items.findAllByActiveTrueOrderBySortOrderAscIdAsc();
        Map<String, TravelStyleItem> itemsByCode = activeItems.stream()
                .collect(Collectors.toMap(TravelStyleItem::getCode, Function.identity()));
        Map<Long, UserTravelStyle> answersByItemId = findAnswers(userId);
        Set<String> updatedCodes = new HashSet<>();

        for (UpdateTravelStyleCommand.Answer update : command.answers()) {
            TravelStyleItem item = itemsByCode.get(update.code());
            if (item == null || !updatedCodes.add(update.code())) {
                throw new BusinessException(CommonErrorCode.INVALID_INPUT);
            }
            applyAnswer(userId, item, update, answersByItemId);
        }
        return toResults(activeItems, answersByItemId);
    }

    private void applyAnswer(long userId, TravelStyleItem item, UpdateTravelStyleCommand.Answer update,
                             Map<Long, UserTravelStyle> answersByItemId) {
        Long itemId = item.getId();
        UserTravelStyle existing = answersByItemId.get(itemId);
        Integer score = update.score();
        if (score == null) {
            if (existing != null) {
                answers.delete(existing);
                answersByItemId.remove(itemId);
            }
        } else if (existing == null) {
            answersByItemId.put(itemId, answers.save(new UserTravelStyle(userId, item, score)));
        } else {
            existing.changeScore(score);
        }
    }

    private static void requireCurrentUser(Optional<User> user, long authVersion) {
        if (user.filter(member -> member.isActiveWithAuthVersion(authVersion)).isEmpty()) {
            throw new BusinessException(CommonErrorCode.UNAUTHENTICATED);
        }
    }

    private Map<Long, UserTravelStyle> findAnswers(long userId) {
        return answers.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserTravelStyle::getItemId, Function.identity()));
    }

    private static List<TravelStyleItemResult> toResults(List<TravelStyleItem> activeItems,
                                                        Map<Long, UserTravelStyle> answersByItemId) {
        return activeItems.stream()
                .map(item -> TravelStyleItemResult.from(item, answersByItemId.get(item.getId())))
                .toList();
    }
}
