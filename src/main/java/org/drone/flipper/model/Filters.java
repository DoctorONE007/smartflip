package org.drone.flipper.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "filters")
public class Filters {

    @Id
    private Long chatId;
    private String telegramUsername;
    private Integer priceLow;
    private Integer priceHigh;
    @Column(name = "m2_price_low")
    private Integer m2PriceLow;
    @Column(name = "m2_price_high")
    private Integer m2PriceHigh;
    private Short floorLow;
    private Short floorHigh;
    @Column(name = "m2_low")
    private Short m2Low;
    @Column(name = "m2_high")
    private Short m2High;
    private Short roomsLow;
    private Short roomsHigh;
    private Short metroMaxTime;
    private String districts;
    private Boolean notFirstFloor;
    private Boolean notLastFloor;
    private Boolean active;
    @Column(name = "next_payment")
    private String nextPayment;
}
