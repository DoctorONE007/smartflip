package org.drone.flipper.mapper;

import org.drone.flipper.model.db.Filters;
import org.drone.flipper.model.request.ConstructFiltersRequest;

public class Mapper {
    public static Filters constructFiltersRequestToFilters(ConstructFiltersRequest request){
        return new Filters(
                request.getChatId(), request.getPriceLow(), request.getPriceHigh(), request.getM2PriceLow(),
                request.getM2PriceHigh(), request.getFloorLow(), request.getFloorHigh(), request.getM2Low(),
                request.getM2High(), request.getRoomsLow(), request.getRoomsHigh(), request.getMetroMaxTime(),
                request.getDistricts(), request.getNotFirstFloor(),request.getNotLastFloor()
        );
    }
}
