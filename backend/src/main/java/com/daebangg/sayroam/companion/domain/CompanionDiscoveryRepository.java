package com.daebangg.sayroam.companion.domain;

import java.util.Optional;

public interface CompanionDiscoveryRepository {
    Optional<CompanionDiscovery> findByFirstSearchIdAndSecondSearchId(long firstSearchId, long secondSearchId);

    void record(CompanionDiscovery discovery);
}
