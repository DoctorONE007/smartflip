package org.drone.flipper.repository;

import jakarta.transaction.Transactional;
import org.drone.flipper.model.db.Flat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlatRepository extends JpaRepository<Flat, Integer> {

    boolean existsByCianId(int cianId);

    @Transactional
    int deleteFlatsByTimeBefore(LocalDateTime time);

    @Query("""
            SELECT f FROM Flat f
            WHERE (:priceLow is null or f.price >= :priceLow)
            AND (:priceHigh is null or f.price <= :priceHigh)
            AND (:m2PriceLow is null or f.priceM2 >= :m2PriceLow)
            AND (:m2PriceHigh is null or f.priceM2 <= :m2PriceHigh)
            AND (:floorLow is null or f.floor >= :floorLow)
            AND (:floorHigh is null or f.floor <= :floorHigh)
            AND (:m2Low is null or f.m2 >= :m2Low)
            AND (:m2High is null or f.m2 <= :m2High)
            AND (f.rooms >= 1)
            AND (:roomsLow is null or f.rooms >= :roomsLow)
            AND (:roomsHigh is null or f.rooms <= :roomsHigh)
            AND (:metroMinWalkTime is null or f.metroMinWalkTime <= :metroMinWalkTime)
            AND (:isFirstFloor is null or f.isFirstFloor = :isFirstFloor)
            AND (:isLastFloor is null or f.isLastFloor = :isLastFloor)
            AND (:districts is null or f.district IN :districts)
            AND (f.time > :time)
            """)
    List<Flat> findActualFlatsByFilters(Integer priceLow, Integer priceHigh, Integer m2PriceLow, Integer m2PriceHigh,
                                        Short floorLow, Short floorHigh, Short m2Low, Short m2High,
                                        Short roomsLow, Short roomsHigh, LocalDateTime time, Short metroMinWalkTime,
                                        Boolean isFirstFloor, Boolean isLastFloor, List<String> districts);
}
