package org.drone.flipper.model.request;

import lombok.Data;

@Data
public class CreateUserRequest {
    private Long chatId;
    private String telegramUsername;
}
