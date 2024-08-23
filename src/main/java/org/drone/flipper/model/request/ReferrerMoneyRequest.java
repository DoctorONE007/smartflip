package org.drone.flipper.model.request;

import lombok.Data;

@Data
public class ReferrerMoneyRequest {
    String userId;
    String cardNumber;
    String amountRub;
}
