package com.daebangg.sayroam.travelstyle.domain;

import java.util.List;

public interface TravelStyleItemRepository {
    List<TravelStyleItem> findAllByActiveTrueOrderBySortOrderAscIdAsc();
}
