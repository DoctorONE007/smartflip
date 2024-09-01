package org.drone.flipper.controller;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.drone.flipper.exception.UserNotFoundException;
import org.drone.flipper.model.request.ReferrerMoneyRequest;
import org.drone.flipper.model.db.Referral;
import org.drone.flipper.model.request.ConstructFiltersRequest;
import org.drone.flipper.model.request.CreateUserRequest;
import org.drone.flipper.model.request.DeactivateUserRequest;
import org.drone.flipper.model.response.IsSubscriptionPaidResponse;
import org.drone.flipper.model.response.NextPaymentResponse;
import org.drone.flipper.service.DbService;
import org.drone.flipper.service.TgMessageSender;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FlatsController {

    private final TgMessageSender tgMessageSender;
    private final ObjectMapper objectMapper;
    private final DbService dbService;
    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @PostConstruct
    private void postConstruct() {
        objectMapper.addHandler(new DeserializationProblemHandler() {
            @Override
            public Object handleWeirdStringValue(DeserializationContext ctxt, Class<?> targetType, String valueToConvert, String failureMsg) {
                return null;
            }

            @Override
            public Object handleWeirdNumberValue(DeserializationContext ctxt, Class<?> targetType, Number valueToConvert, String failureMsg) {
                return null;
            }
        });
    }

    @PostMapping("createUser")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        log.info("createUser: {}", request.toString());
        dbService.createUser(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("deactivateUser")
    public ResponseEntity<?> deactivateUser(@RequestBody DeactivateUserRequest request) {
        log.info("deactivateUser: {}", request.toString());
        dbService.deactivateUser(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("constructFilters")
    public ResponseEntity<?> constructFilters(@RequestBody ConstructFiltersRequest request) {
        log.info("constructFilters: {}", request.toString());

        dbService.constructFilters(request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("paymentWebhook")
    public ResponseEntity<?> paymentWebhook(@RequestParam String nextPayment, @RequestParam String consumerTelegramId) {
        //todo возможно надо задержку тк юзер может не создаться - done/test
        //todo при закрытии приходит пустая строка в nextPayment
        log.info("paymentWebhook: {}, {}", nextPayment, consumerTelegramId);

        if (consumerTelegramId != null && !consumerTelegramId.isEmpty()) {
            if(nextPayment != null && nextPayment.isEmpty()){
                nextPayment = "-";
            }
            try {
                dbService.setNextPaymentByChatId(consumerTelegramId, nextPayment);
            } catch (UserNotFoundException e){
                log.error("UserNotFoundException: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("refMoneyToPay")
    public ResponseEntity<?> refMoneyToPay(@RequestBody ReferrerMoneyRequest request) {
        log.info("refMoneyToPay: {}", request.toString());

        dbService.saveReferral(new Referral(request.getUserId(), request.getPhoneNumber(), request.getBankName(), request.getAmountRub()));
        tgMessageSender.sendToRefChat(request);

        return ResponseEntity.ok().build();
    }

    @GetMapping("getNextPayment")
    public ResponseEntity<?> getNextPayment(@RequestParam String chatId) {
        log.info("getNextPayment: {}", chatId);

        NextPaymentResponse nextPaymentResponse = new NextPaymentResponse(dbService.getNextPayment(chatId));

        log.info("nextPayment: {}", nextPaymentResponse.getNextPayment());

        return ResponseEntity.ok().body(nextPaymentResponse);
    }

    @GetMapping("isSubscriptionPaid")
    public ResponseEntity<?> isSubscriptionPaid(@RequestParam String chatId) {
        log.info("isSubscriptionPaid: {}", chatId);

        String nextPayment = dbService.getNextPayment(chatId);
        IsSubscriptionPaidResponse isSubscriptionPaidResponse;
        if (nextPayment == null || nextPayment.isEmpty()) {
            isSubscriptionPaidResponse = new IsSubscriptionPaidResponse("false");
        } else {
            LocalDate date = LocalDate.parse(nextPayment, formatter);
            if (date.isAfter(LocalDate.now().plusDays(5))) {
                isSubscriptionPaidResponse = new IsSubscriptionPaidResponse("true");
            } else {
                isSubscriptionPaidResponse = new IsSubscriptionPaidResponse("false");
            }
        }

        log.info("isSubscriptionPaid: {}", isSubscriptionPaidResponse.getIsSubscriptionPaid());

        return ResponseEntity.ok().body(isSubscriptionPaidResponse);
    }
}

