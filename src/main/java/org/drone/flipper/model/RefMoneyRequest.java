package org.drone.flipper.model;

import lombok.Data;

@Data
public class RefMoneyRequest {
    String userId;
    String cardNumber;
    String amountRub;
}
