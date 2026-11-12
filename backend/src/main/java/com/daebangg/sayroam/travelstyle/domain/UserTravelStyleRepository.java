package com.daebangg.sayroam.travelstyle.domain;

import java.util.List;

public interface UserTravelStyleRepository {
    List<UserTravelStyle> findByUserId(long userId);

    <S extends UserTravelStyle> S save(S answer);

    void delete(UserTravelStyle answer);
}
