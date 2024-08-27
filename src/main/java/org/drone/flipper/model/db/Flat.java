package org.drone.flipper.model.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "flats")
public class Flat {

    @Id
    private int cianId;
    private int price;
    private int lowCianPrice;
    @Column(columnDefinition = "real")
    private double priceGap;
    private int viewsCount;
    @Column(name = "price_m2")
    private int priceM2;
    @Column(columnDefinition = "text")
    private String address;
    @Column(columnDefinition = "text")
    private String metro;
    @Column(columnDefinition = "text")
    private String district;
    private short floor;
    private short m2;
    private short rooms;
    private Short metroMinWalkTime;
    private Boolean isFirstFloor;
    private Boolean isLastFloor;
    @Column(length = 30, columnDefinition = "timestamp without time zone default CURRENT_TIMESTAMP")
    private LocalDateTime time;
    private int buildingFloors;
    @Column(columnDefinition = "text")
    private String nearbyFlatsMessage;
}
