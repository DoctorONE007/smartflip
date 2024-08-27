package org.drone.flipper.model.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "filters")
@AllArgsConstructor
@NoArgsConstructor
public class Filters {

    @Id
    private Long chatId;
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
    @Column(columnDefinition = "text")
    private String districts;
    private Boolean notFirstFloor;
    private Boolean notLastFloor;
}
