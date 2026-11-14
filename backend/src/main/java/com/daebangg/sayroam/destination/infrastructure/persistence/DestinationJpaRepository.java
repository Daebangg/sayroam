package com.daebangg.sayroam.destination.infrastructure.persistence;

import com.daebangg.sayroam.destination.domain.Destination;
import com.daebangg.sayroam.destination.domain.DestinationRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DestinationJpaRepository extends DestinationRepository, JpaRepository<Destination, Long> {

    @Override
    @Query("""
            select d from Destination d
            where d.active = true and (
                :query = ''
                or locate(:query, replace(d.name, ' ', '')) > 0
                or locate(:query, replace(d.searchName, ' ', '')) > 0
            )
            order by d.name, d.id
            """)
    List<Destination> findActiveByName(@Param("query") String query);
}
