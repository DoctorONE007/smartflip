package org.drone.flipper.model.request;

import lombok.Data;

@Data
public class ReferrerMoneyRequest {
    String userId;
    String phoneNumber;
    String bankName;
    String amountRub;
}
