package org.drone.flipper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.drone.flipper.exception.UserNotFoundException;
import org.drone.flipper.mapper.Mapper;
import org.drone.flipper.model.db.Filters;
import org.drone.flipper.model.db.Flat;
import org.drone.flipper.model.db.Referral;
import org.drone.flipper.model.db.User;
import org.drone.flipper.model.request.ConstructFiltersRequest;
import org.drone.flipper.model.request.CreateUserRequest;
import org.drone.flipper.model.request.DeactivateUserRequest;
import org.drone.flipper.model.response.ChatMemberResponse;
import org.drone.flipper.properties.Properties;
import org.drone.flipper.repository.FiltersRepository;
import org.drone.flipper.repository.FlatRepository;
import org.drone.flipper.repository.RefRepository;
import org.drone.flipper.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DbService {

    private final FlatRepository flatRepository;
    private final FiltersRepository filtersRepository;
    private final RefRepository refRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final Properties properties;

    public List<Flat> findActualFlats(Filters request) {
        return flatRepository.findActualFlatsByFilters(request.getPriceLow(), request.getPriceHigh(),
                request.getM2PriceLow(), request.getM2PriceHigh(), request.getFloorLow(), request.getFloorHigh(), request.getM2Low(),
                request.getM2High(), request.getRoomsLow(), request.getRoomsHigh(), LocalDateTime.now().minusMinutes(15), request.getMetroMaxTime(),
                request.getNotFirstFloor() == null || !request.getNotFirstFloor() ? null : false,
                request.getNotLastFloor() == null || !request.getNotLastFloor() ? null : false,
                (request.getDistricts() == null || request.getDistricts().trim().isEmpty()) ? null : List.of(request.getDistricts().split(" ")));
    }

    public Optional<Filters> getFiltersByChatId(Long chatId) {
        return filtersRepository.findById(chatId);
    }

    public void constructFilters(ConstructFiltersRequest request) {

        String districts = request.getDistricts();
        districts = districts.replaceAll("0", "").replaceAll(" +", " ").trim();
        request.setDistricts(districts.isEmpty() ? null : districts);

        Filters filters = Mapper.constructFiltersRequestToFilters(request);

        filtersRepository.save(filters);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void saveReferral(Referral referral) {
        refRepository.save(referral);
    }

    @Retryable(retryFor = UserNotFoundException.class, maxAttempts = 5, backoff = @Backoff(delay = 5000))
    public void setNextPaymentByChatId(String chatId, String nextPayment) {
        Optional<User> userOptional = userRepository.findById(Long.valueOf(chatId));
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setNextPayment(nextPayment);
            userRepository.save(user);
        } else {
            throw new UserNotFoundException(chatId);
        }
    }

    public String getNextPayment(String chatId) {
        Optional<User> user = userRepository.findById(Long.valueOf(chatId));
        if (user.isPresent()) {
            return user.get().getNextPayment();
        } else {
            return "null";
        }
    }

    public void createUser(CreateUserRequest request) {
        Optional<User> user = userRepository.findById(request.getChatId());
        if (user.isEmpty()) {
            userRepository.save(new User(request.getChatId(), request.getTelegramUsername(), Boolean.TRUE, null));
            Filters emptyFilters = new Filters();
            emptyFilters.setChatId(request.getChatId());
            filtersRepository.save(emptyFilters);
        } else {
            User activeUser = user.get();
            if (activeUser.getIsActive() == null || !activeUser.getIsActive()) {
                activeUser.setIsActive(Boolean.TRUE);
                userRepository.save(activeUser);
            }
        }

    }

    public void deactivateUser(DeactivateUserRequest request) {
        Optional<User> user = userRepository.findById(request.getChatId());
        if (user.isPresent()) {
            User diactivatedUser = user.get();
            diactivatedUser.setIsActive(Boolean.FALSE);
            userRepository.save(diactivatedUser);
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void deleteOldFlats() {
        log.info("start deleting old flats");
        int numberOfDeletedFlats = flatRepository.deleteFlatsByTimeBefore(LocalDateTime.now().minusDays(30));
        log.info("deleted {} flats", numberOfDeletedFlats);
    }

    //todo вернуть когда вернем оплату
//    @Scheduled(cron = "0 0 0 * * *")
    public void deactivateUsers() {
        List<User> users = getAllUsers();
        for (User user : users) {
            if (user.getIsActive() == null || !user.getIsActive()) {
                continue;
            }
            Map<String, String> uriVars = new HashMap<>();
            uriVars.put("user_id", user.getChatId().toString());
            ResponseEntity<ChatMemberResponse> response =
                    restTemplate.getForEntity("https://api.telegram.org/bot" + properties.getTgKey() + "/getChatMember?chat_id=-1002236926120&user_id={user_id}", ChatMemberResponse.class, uriVars);
            if (response.getStatusCode().is2xxSuccessful()) {
                String status = Objects.requireNonNull(response.getBody()).getResult().getStatus();
                if (status != null && !status.equals("creator") && !status.equals("administrator") && !status.equals("member")) {
                    user.setIsActive(false);
                    userRepository.save(user);
                }
            }
        }
    }
}
