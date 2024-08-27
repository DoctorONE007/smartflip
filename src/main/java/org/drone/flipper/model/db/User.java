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
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private Long chatId;
    @Column(columnDefinition = "text")
    private String telegramUsername;
    private Boolean isActive;
    @Column(name = "next_payment", columnDefinition = "text")
    private String nextPayment;
}