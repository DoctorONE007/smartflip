package org.drone.flipper.model;

import lombok.Data;

import java.util.List;

@Data
public class GetFlatsRequest {

    Long chatId;
    Integer priceLow;
    Integer priceHigh;
    Integer m2PriceLow;
    Integer m2PriceHigh;
    Short floorLow;
    Short floorHigh;
    Short m2Low;
    Short m2High;
    Short roomsLow;
    Short roomsHigh;
    Short metroMaxTime;
    List<String> districts;
    Boolean notFirstFloor;
    Boolean notLastFloor;
}