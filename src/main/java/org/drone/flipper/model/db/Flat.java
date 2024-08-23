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
    private double priceGap;
    private int viewsCount;
    @Column(name = "price_m2")
    private int priceM2;
    private String address;
    private String metro;
    private String district;
    private short floor;
    private short m2;
    private short rooms;
//    private String photos;
    private Short metroMinWalkTime;
    private Boolean isFirstFloor;
    private Boolean isLastFloor;
    private LocalDateTime time;
    private int buildingFloors;
    private String nearbyFlatsMessage;
}
