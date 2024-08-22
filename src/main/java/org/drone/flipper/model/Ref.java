package org.drone.flipper.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "ref")
@NoArgsConstructor
public class Ref {

    public Ref(String userId, String cardNumber, String amountRub) {
        this.userId = userId;
        this.cardNumber = cardNumber;
        this.amountRub = amountRub;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;
    private String userId;
    private String cardNumber;
    private String amountRub;
}
