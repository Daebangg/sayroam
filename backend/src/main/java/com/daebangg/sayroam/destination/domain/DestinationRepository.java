package com.daebangg.sayroam.destination.domain;

import java.util.List;

public interface DestinationRepository {
    List<Destination> findActiveByName(String query);
}
