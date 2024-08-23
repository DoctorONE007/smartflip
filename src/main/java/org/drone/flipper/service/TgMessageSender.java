package org.drone.flipper.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.drone.flipper.model.db.Filters;
import org.drone.flipper.model.db.Flat;
import org.drone.flipper.model.db.User;
import org.drone.flipper.model.request.ReferrerMoneyRequest;
import org.drone.flipper.properties.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TgMessageSender {

    private static final Logger log = LoggerFactory.getLogger(TgMessageSender.class);
    private final DbService dbService;
    private final Properties properties;
    private final RestTemplate restTemplate;
    private final HttpHeaders headers = new HttpHeaders();

    private final NumberFormat formatter = NumberFormat.getInstance(Locale.of("ru"));

    @PostConstruct
    private void postConstruct() {
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Scheduled(cron = "0 */15 * * * *")
    public void sendActualFlats() {
        log.info("start sending flats");
        List<User> users = dbService.getAllUsers();
        for (User user : users) {
            if (user.getIsActive() == null || !user.getIsActive()) {
                continue;
            }

            Optional<Filters> filters = dbService.getFiltersByChatId(user.getChatId());
            if (filters.isEmpty()) {
                log.error("filter not found for chat_id {}", user.getChatId());
                continue;
            }
            List<Flat> actualFlats = dbService.findActualFlats(filters.get());

            actualFlats.forEach(
                    flat -> {
                        String message = "\uD83D\uDCCD" + " *" + flat.getAddress() + "*" + "\n" +
                                "\n" +
                                "\uD83C\uDFF7️ " + "*" + formatter.format(flat.getPrice()) + "*" + " *₽* (" + formatter.format(flat.getPriceM2()) + " ₽/м²)\n" +
                                "\uD83D\uDECF️ Количество комнат: " + "*" + flat.getRooms() + "*" + "\n" +
                                "\uD83C\uDFE0 Площадь: " + "*" + flat.getM2() + "м²" + "*" + " \n" +
                                "↕️ Этаж: " + "*" + flat.getFloor() + "/" + flat.getBuildingFloors() + "*" + "\n" +
                                "\uD83D\uDE87 Метро: " + makeBoldMetro(flat.getMetro()) + "\n" +
                                "\n" +
                                "Изучить объявление \uD83D\uDC47\n" +
                                "https://www.cian.ru/sale/flat/" + flat.getCianId() + "\n\n" +
                                flat.getNearbyFlatsMessage();
                        message = replaceAllMarkdownChars(message);
                        HttpEntity<String> tgRequest = new HttpEntity<>("{\"chat_id\": \"" + filters.get().getChatId() + "\", \"text\": \"" + message + "\"," + "\"parse_mode\": \"MarkdownV2\"" + "}", headers);
                        restTemplate.postForEntity("https://api.telegram.org/bot" + properties.getTgKey() + "/sendMessage", tgRequest, String.class);
                    }
            );

        }
    }

    private String makeBoldMetro(String metro) {
        String[] arr = metro.trim().split("\\((.*?)\\)");
        for (String s : arr) {
            metro = metro.replace(s, "*" + s + "*");
        }
        return metro;
    }

    private String replaceAllMarkdownChars(String message) {
        return message
                .replaceAll("_", "\\\\\\\\_")
                .replaceAll("\\[", "\\\\\\\\[")
                .replaceAll("]", "\\\\\\\\]")
                .replaceAll("\\(", "\\\\\\\\(")
                .replaceAll("\\)", "\\\\\\\\)")
                .replaceAll("~", "\\\\\\\\~")
                .replaceAll("`", "\\\\\\\\`")
                .replaceAll(">", "\\\\\\\\>")
                .replaceAll("#", "\\\\\\\\#")
                .replaceAll("\\+", "\\\\\\\\+")
                .replaceAll("-", "\\\\\\\\-")
                .replaceAll("=", "\\\\\\\\=")
                .replaceAll("\\|", "\\\\\\\\|")
                .replaceAll("\\{", "\\\\\\\\{")
                .replaceAll("}", "\\\\\\\\}")
                .replaceAll("\\.", "\\\\\\\\.")
                .replaceAll("!", "\\\\\\\\!");
    }

    public void sendToRefChat(ReferrerMoneyRequest request) {
        String message = "Запрос на вывод средств от " + request.getUserId() + " на сумму " + request.getAmountRub() + " по карте " + request.getCardNumber();
        HttpEntity<String> tgRequest = new HttpEntity<>("{\"chat_id\": \"" + "-4227941390" + "\", \"text\": \"" + message + "\"" + "}", headers);
        restTemplate.postForEntity("https://api.telegram.org/bot" + properties.getTgKey() + "/sendMessage", tgRequest, String.class);
    }
}
