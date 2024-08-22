package org.drone.flipper.controller;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.drone.flipper.model.Filters;
import org.drone.flipper.model.GetFlatsRequest;
import org.drone.flipper.model.Ref;
import org.drone.flipper.model.RefMoneyRequest;
import org.drone.flipper.service.DbService;
import org.drone.flipper.service.TgMessageSender;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FlatsController {

    private final TgMessageSender tgMessageSender;
    private final ObjectMapper objectMapper;
    private final DbService dbService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

//    @PostMapping("getFlats")
//    public ResponseEntity<?> getFlats(@RequestBody GetFlatsRequest request) {
//        log.info(request.toString());
//
//        tgMessageSender.sendActualFlats(request);
//
//        return ResponseEntity.ok().build();
//    }

    @PostMapping("constructUser")
    public ResponseEntity<?> constructUser(@RequestBody Filters request) {
        log.info(request.toString());

        dbService.constructUser(request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("paymentWebhook")
    public ResponseEntity<?> paymentWebhook(@RequestParam String nextPayment, @RequestParam String consumerTelegramId) {
        log.info("paymentWebhook: {}, {}", nextPayment, consumerTelegramId);
        if (consumerTelegramId != null && !consumerTelegramId.isEmpty()) {
            dbService.setNextPaymentInFiltersByChatId(consumerTelegramId, nextPayment);
        }

        return ResponseEntity.ok().build();
    }


    @PostMapping("refMoneyToPay")
    public ResponseEntity<?> refMoneyToPay(@RequestBody RefMoneyRequest request) {
        log.info(request.toString());

        dbService.saveRef(new Ref(request.getUserId(), request.getCardNumber(), request.getAmountRub()));
        tgMessageSender.sendToRefChat(request);

        return ResponseEntity.ok().build();
    }

    @GetMapping("getNextPayment")
    public ResponseEntity<?> getNextPayment(@RequestParam String chatId) {
        log.info("getNextPayment: {}", chatId);

        NextPayment nextPayment = new NextPayment(dbService.getNextPayment(chatId));
        log.info("nextPayment: {}", nextPayment.getNextPayment());
        return ResponseEntity.ok().body(nextPayment);
    }

    @GetMapping("isSubscriptionPaid")
    public ResponseEntity<?> isSubscriptionPaid(@RequestParam String chatId) {
        log.info("isSubscriptionPaid: {}", chatId);

        String nextPayment = dbService.getNextPayment(chatId);
        IsSubscriptionPaid isSubscriptionPaid;
        if (nextPayment == null || nextPayment.isEmpty()) {
            isSubscriptionPaid = new IsSubscriptionPaid("false");
        } else {
            LocalDate date = LocalDate.parse(nextPayment, formatter);
            if (date.isAfter(LocalDate.now().plusDays(5))) {
                isSubscriptionPaid = new IsSubscriptionPaid("true");
            } else {
                isSubscriptionPaid = new IsSubscriptionPaid("false");
            }
        }
        log.info("isSubscriptionPaid: {}", isSubscriptionPaid.getIsSubscriptionPaid());
        return ResponseEntity.ok().body(isSubscriptionPaid);
    }
}

@Data
@AllArgsConstructor
class NextPayment {
    String nextPayment;
}

@Data
@AllArgsConstructor
class IsSubscriptionPaid {
    String isSubscriptionPaid;
}