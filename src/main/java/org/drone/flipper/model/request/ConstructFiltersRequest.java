package org.drone.flipper.model.request;

import lombok.Data;

@Data
public class ConstructFiltersRequest {
    private Long chatId;
    private Integer priceLow;
    private Integer priceHigh;
    private Integer m2PriceLow;
    private Integer m2PriceHigh;
    private Short floorLow;
    private Short floorHigh;
    private Short m2Low;
    private Short m2High;
    private Short roomsLow;
    private Short roomsHigh;
    private Short metroMaxTime;
    private String districts;
    private Boolean notFirstFloor;
    private Boolean notLastFloor;
}
