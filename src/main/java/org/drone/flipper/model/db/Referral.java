package org.drone.flipper.model.db;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "referrals")
@NoArgsConstructor
public class Referral {

    public Referral(String userId, String phoneNumber, String bankName, String amountRub) {
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.bankName = bankName;
        this.amountRub = amountRub;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;
    @Column(columnDefinition = "text")
    private String userId;
    @Column(columnDefinition = "text")
    private String phoneNumber;
    @Column(columnDefinition = "text")
    private String bankName;
    @Column(columnDefinition = "text")
    private String amountRub;
}
