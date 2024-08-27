package org.drone.flipper.model.db;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "referrals")
@NoArgsConstructor
public class Referral {

    public Referral(String userId, String cardNumber, String amountRub) {
        this.userId = userId;
        this.cardNumber = cardNumber;
        this.amountRub = amountRub;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;
    @Column(columnDefinition = "text")
    private String userId;
    @Column(columnDefinition = "text")
    private String cardNumber;
    @Column(columnDefinition = "text")
    private String amountRub;
}
