package org.drone.flipper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.drone.flipper.model.*;
import org.drone.flipper.properties.Properties;
import org.drone.flipper.repository.FiltersRepository;
import org.drone.flipper.repository.FlatRepository;
import org.drone.flipper.repository.RefRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
    private final RestTemplate restTemplate;
    private final Properties properties;
    private final HttpHeaders headers = new HttpHeaders();

    public List<Flat> findActualFlats(GetFlatsRequest request) {
        return flatRepository.findActualFlatsByFilters(request.getPriceLow(), request.getPriceHigh(),
                request.getM2PriceLow(), request.getM2PriceHigh(), request.getFloorLow(), request.getFloorHigh(),
                request.getM2Low(), request.getM2High(), request.getRoomsLow(), request.getRoomsHigh(), LocalDateTime.now().minusMinutes(15),
                request.getMetroMaxTime(), request.getNotFirstFloor() == null ? null : !request.getNotFirstFloor(),
                request.getNotLastFloor() == null ? null : !request.getNotLastFloor(), request.getDistricts());
    }

    public void constructUser(Filters request) {

        String districts = request.getDistricts();
        districts = districts.replaceAll("0", "").replaceAll(" +", " ").trim();
        request.setDistricts(districts.isEmpty() ? null : districts);

        Optional<Filters> filters = filtersRepository.findById(request.getChatId());
        filters.ifPresent(value -> request.setNextPayment(value.getNextPayment()));

        filtersRepository.save(request);
    }

    public List<Filters> getAllUsers(){
        return filtersRepository.findAll();
    }

    public void saveRef(Ref ref){
        refRepository.save(ref);
    }

    public void setNextPaymentInFiltersByChatId(String chatId, String nextPayment){
        Optional<Filters> filters = filtersRepository.findById(Long.valueOf(chatId));
        if(filters.isPresent()){
            Filters filter = filters.get();
            filter.setNextPayment(nextPayment);
            filtersRepository.save(filter);
        }
    }

    public String getNextPayment(String chatId){
        Optional<Filters> filters = filtersRepository.findById(Long.valueOf(chatId));
        if(filters.isPresent()){
            return filters.get().getNextPayment();
        } else {
            return "null";
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void deleteOldFlats(){
        log.info("start deleting old flats");
        int numberOfDeletedFlats = flatRepository.deleteFlatsByTimeBefore(LocalDateTime.now().minusDays(10));
        log.info("deleted {} flats", numberOfDeletedFlats);
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void deactivateUsers(){
        List<Filters> filters = getAllUsers();
        for (Filters filter : filters) {
            if (filter.getActive() == null || !filter.getActive()) {
                continue;
            }
            Map<String, String> uriVars= new HashMap<>();
            uriVars.put("user_id", filter.getChatId().toString());
            ResponseEntity<ChatMember> response = restTemplate.getForEntity("https://api.telegram.org/bot" + properties.getTgKey() + "/getChatMember?chat_id=-1002236926120&user_id={user_id}", ChatMember.class, uriVars);
            if (response.getStatusCode().is2xxSuccessful()) {
                String status = Objects.requireNonNull(response.getBody()).getResult().getStatus();
                if(status!= null && !status.equals("creator") && !status.equals("administrator") && !status.equals("member")) {
                    filter.setActive(false);
                    filtersRepository.save(filter);
                }
            }
        }
    }
}
