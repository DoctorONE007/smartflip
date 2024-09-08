package org.drone.flipper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.drone.flipper.model.db.Flat;
import org.drone.flipper.repository.FlatRepository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//todo подумать с квартирами без цены - done/test
//todo подумать с квартирами в начале новой страницы, дубль и прерывание (случай с парсингом нескольких страниц)
//todo подумать с квартирами, которые не в базе, но мы их уже парсили (случай с парсингом нескольких страниц)
//todo добавить телефон
//todo в квартирах где до метро только на машине проставляется null
//todo бесконечный цикл при отправке запросов в циан - done/test


@Slf4j
@Service
@RequiredArgsConstructor
public class CianParser {

    private final FlatRepository flatRepository;
    private final NumberFormat formatter = NumberFormat.getInstance(Locale.of("ru"));
    private final Set<String> finalCianIds = new HashSet<>();
    private final Set<String> analyzedCianIds = new HashSet<>();
    private Set<String> previouslyAnalyzedCianIds = new HashSet<>();
    private final Map<String, Integer> priceParsingErrorCianIdsMap = new HashMap<>();

    @Scheduled(cron = "0 */6 * * * *")
    public void start() {
        analyzedCianIds.clear();
        finalCianIds.clear();
        for (int i = 1; i < 2; i++) {
            log.info("Parsing page {}", i);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(createRequest(i)))
                    .GET()
                    .build();
            String response = sendRequest(request);
            if (response == null) {
                continue;
            }

            Set<String> cianIds = parsePageWithAllFlats(response);
            log.info("priceParsingErrorCianIdsMap {}", priceParsingErrorCianIdsMap.keySet());
            cianIds.addAll(priceParsingErrorCianIdsMap.keySet());
            analyzedCianIds.addAll(cianIds);
            cianIds.removeAll(previouslyAnalyzedCianIds);
            log.info("Going to parse {}", cianIds);

            for (String cianId : cianIds) {
                parseFlat(cianId);
            }

        }
        log.info("The end of parsing. Chosen flats {}", finalCianIds);
        previouslyAnalyzedCianIds = new HashSet<>(analyzedCianIds);
    }

    public String createRequest(int pageNum) {
        String url = "https://www.cian.ru/cat.php?";
        String currency = "currency=2&"; //ХЗ ВАЛЮТА КАКАЯ-ТО МБ ВЫЧИСЛЕНИЕ СТОИМОСТИ РУБ ИЛИ USD...
        String engineVersion = "engine_version=2&"; // ХЗ ЯДРО ОБРАБОТЧИКА МБ
        String dealType = "deal_type=sale&";//ПРОДАЖА
        String demolishedInMoscowProgramm = "demolished_in_moscow_programm=0&"; //ПОД СНОС
        String electronicTrading = "electronic_trading=2&"; // ТОРГИ И ЗАЛОГОВАЯ НЕДВИЖИМОСТЬ
        String flatShare = "flat_share=2&"; // ДОЛИ НЕ ПОКАЗЫВАТЬ
//        String m2MaxPrice = "m2=1&maxprice=400000&"; // МАКСИМАЛЬНАЯ ЦЕНА ЗА М2
        String m2MaxPrice = "maxprice=30000000&"; // МАКСИМАЛЬНАЯ ЦЕНА ОБЩАЯ
        String isFirstFloor = "is_first_floor=0&"; // ПЕРВЫЙ ЭТАЖ?
//        String maxtarea = "maxtarea=70&"; // МАКСИМАЛЬНАЯ ПЛОЩАДЬ
        String maxtarea = "maxtarea=120&"; // МАКСИМАЛЬНАЯ ПЛОЩАДЬ
//        String footMin = "foot_min=20&"; // ВРЕМЯ ДО МЕТРО
        String footMin = "foot_min=40&"; // ВРЕМЯ ДО МЕТРО
        String onlyFoot = "only_foot=2&"; // МЕТРО ПЕШКОМ
//        String metro = "metro[0]=1&metro[100]=108&metro[101]=109&metro[102]=110&metro[103]=111&metro[104]=112&metro[105]=113&metro[106]=114&metro[107]=115&metro[108]=116&metro[109]=117&metro[10]=11&" +
//                "metro[110]=118&metro[111]=119&metro[112]=120&metro[113]=121&metro[114]=122&metro[115]=123&metro[116]=124&metro[117]=125&metro[118]=126&metro[119]=127&metro[11]=12&metro[120]=128&metro[121]=129&" +
//                "metro[122]=130&metro[123]=131&metro[124]=132&metro[125]=133&metro[126]=134&metro[127]=135&metro[128]=137&metro[129]=140&metro[12]=14&metro[130]=141&metro[131]=142&metro[132]=143&metro[133]=144&" +
//                "metro[134]=145&metro[135]=146&metro[136]=147&metro[137]=148&metro[138]=149&metro[139]=150&metro[13]=15&metro[140]=151&metro[141]=152&metro[142]=153&metro[143]=154&metro[144]=155&metro[145]=156&" +
//                "metro[146]=157&metro[147]=158&metro[148]=159&metro[149]=228&metro[14]=16&metro[150]=229&metro[151]=236&metro[152]=237&metro[153]=238&metro[154]=240&metro[155]=243&metro[156]=272&metro[157]=274&" +
//                "metro[158]=275&metro[159]=276&metro[15]=17&metro[160]=277&metro[161]=278&metro[162]=279&metro[163]=280&metro[164]=281&metro[165]=283&metro[166]=284&metro[167]=285&metro[168]=286&metro[169]=287&" +
//                "metro[16]=18&metro[170]=289&metro[171]=290&metro[172]=291&metro[173]=292&metro[174]=293&metro[175]=294&metro[176]=295&metro[177]=296&metro[178]=297&metro[179]=298&metro[17]=19&metro[180]=299&" +
//                "metro[181]=300&metro[182]=301&metro[183]=302&metro[184]=303&metro[185]=304&metro[186]=305&metro[187]=306&metro[188]=307&metro[189]=308&metro[18]=20&metro[190]=309&metro[191]=310&metro[192]=311&" +
//                "metro[193]=337&metro[194]=338&metro[195]=339&metro[196]=349&metro[197]=350&metro[198]=351&metro[199]=352&metro[19]=21&metro[1]=2&metro[200]=353&metro[201]=354&metro[202]=361&metro[203]=362&" +
//                "metro[204]=363&metro[205]=364&metro[206]=369&metro[207]=374&metro[208]=375&metro[209]=376&metro[20]=22&metro[210]=377&metro[211]=384&metro[212]=385&metro[213]=386&metro[214]=387&metro[215]=388&" +
//                "metro[216]=389&metro[217]=390&metro[218]=391&metro[219]=393&metro[21]=26&metro[220]=394&metro[221]=395&metro[222]=396&metro[223]=400&metro[224]=423&metro[225]=424&metro[226]=425&metro[227]=426&" +
//                "metro[228]=427&metro[229]=428&metro[22]=27&metro[230]=429&metro[231]=430&metro[232]=439&metro[233]=440&metro[234]=441&metro[235]=443&metro[236]=444&metro[237]=445&metro[238]=446&metro[239]=447&" +
//                "metro[23]=28&metro[240]=448&metro[241]=449&metro[242]=450&metro[243]=451&metro[244]=452&metro[245]=453&metro[246]=454&metro[247]=463&metro[248]=466&metro[249]=467&metro[24]=29&metro[250]=470&" +
//                "metro[251]=472&metro[252]=473&metro[253]=474&metro[254]=477&metro[255]=509&metro[256]=511&metro[257]=512&metro[258]=513&metro[259]=514&metro[25]=30&metro[260]=515&metro[261]=516&metro[262]=517&" +
//                "metro[263]=518&metro[264]=519&metro[265]=520&metro[266]=521&metro[267]=524&metro[268]=537&metro[269]=540&metro[26]=31&metro[270]=541&metro[271]=548&metro[272]=549&metro[273]=554&metro[274]=555&" +
//                "metro[275]=556&metro[276]=559&metro[277]=560&metro[278]=563&metro[279]=565&metro[27]=32&metro[280]=567&metro[281]=568&metro[282]=569&metro[283]=571&metro[284]=572&metro[285]=573&metro[286]=574&" +
//                "metro[28]=33&metro[29]=34&metro[2]=3&metro[30]=35&metro[31]=36&metro[32]=37&metro[33]=38&metro[34]=40&metro[35]=41&metro[36]=42&metro[37]=43&metro[38]=44&metro[39]=45&metro[3]=4&metro[40]=46&" +
//                "metro[41]=47&metro[42]=48&metro[43]=49&metro[44]=50&metro[45]=51&metro[46]=53&metro[47]=54&metro[48]=55&metro[49]=56&metro[4]=5&metro[50]=57&metro[51]=58&metro[52]=59&metro[53]=60&metro[54]=61&" +
//                "metro[55]=62&metro[56]=63&metro[57]=64&metro[58]=65&metro[59]=66&metro[5]=6&metro[60]=67&metro[61]=68&metro[62]=69&metro[63]=70&metro[64]=71&metro[65]=72&metro[66]=73&metro[67]=74&metro[68]=75&" +
//                "metro[69]=76&metro[6]=7&metro[70]=77&metro[71]=78&metro[72]=79&metro[73]=80&metro[74]=81&metro[75]=83&metro[76]=84&metro[77]=85&metro[78]=86&metro[79]=87&metro[7]=8&metro[80]=88&metro[81]=89&" +
//                "metro[82]=90&metro[83]=91&metro[84]=92&metro[85]=93&metro[86]=94&metro[87]=95&metro[88]=96&metro[89]=97&metro[8]=9&metro[90]=98&metro[91]=99&metro[92]=100&metro[93]=101&metro[94]=102&metro[95]=103&" +
//                "metro[96]=104&metro[97]=105&metro[98]=106&metro[99]=107&metro[9]=10&"; // СТАНЦИИ МЕТРО
        String metro = "metro[0]=1&metro[100]=101&metro[101]=102&metro[102]=103&metro[103]=104&metro[104]=105&metro[105]=106&metro[106]=107&metro[107]=108&metro[108]=109&metro[109]=110&metro[10]=11&metro[110]=111&metro[111]=112&metro[112]=113&metro[113]=114&metro[114]=115&metro[115]=116&metro[116]=117&metro[117]=118&metro[118]=119&metro[119]=120&metro[11]=12&metro[120]=121&metro[121]=122&metro[122]=123&metro[123]=124&metro[124]=125&metro[125]=126&metro[126]=127&metro[127]=128&metro[128]=129&metro[129]=130&metro[12]=13&metro[130]=131&metro[131]=132&metro[132]=133&metro[133]=134&metro[134]=135&metro[135]=136&metro[136]=137&metro[137]=138&metro[138]=139&metro[139]=140&metro[13]=14&metro[140]=141&metro[141]=142&metro[142]=143&metro[143]=144&metro[144]=145&metro[145]=146&metro[146]=147&metro[147]=148&metro[148]=149&metro[149]=150&metro[14]=15&metro[150]=151&metro[151]=152&metro[152]=153&metro[153]=154&metro[154]=155&metro[155]=156&metro[156]=157&metro[157]=158&metro[158]=159&metro[159]=228&metro[15]=16&metro[160]=229&metro[161]=233&metro[162]=234&metro[163]=235&metro[164]=236&metro[165]=237&metro[166]=238&metro[167]=239&metro[168]=240&metro[169]=243&metro[16]=17&metro[170]=244&metro[171]=245&metro[172]=270&metro[173]=271&metro[174]=272&metro[175]=273&metro[176]=274&metro[177]=275&metro[178]=276&metro[179]=277&metro[17]=18&metro[180]=278&metro[181]=279&metro[182]=280&metro[183]=281&metro[184]=282&metro[185]=283&metro[186]=284&metro[187]=285&metro[188]=286&metro[189]=287&metro[18]=19&metro[190]=289&metro[191]=290&metro[192]=291&metro[193]=292&metro[194]=293&metro[195]=294&metro[196]=295&metro[197]=296&metro[198]=297&metro[199]=298&metro[19]=20&metro[1]=2&metro[200]=299&metro[201]=300&metro[202]=301&metro[203]=302&metro[204]=303&metro[205]=304&metro[206]=305&metro[207]=306&metro[208]=307&metro[209]=308&metro[20]=21&metro[210]=309&metro[211]=310&metro[212]=311&metro[213]=337&metro[214]=338&metro[215]=339&metro[216]=349&metro[217]=350&metro[218]=351&metro[219]=352&metro[21]=22&metro[220]=353&metro[221]=354&metro[222]=361&metro[223]=362&metro[224]=363&metro[225]=364&metro[226]=365&metro[227]=366&metro[228]=367&metro[229]=369&metro[22]=23&metro[230]=370&metro[231]=371&metro[232]=372&metro[233]=373&metro[234]=374&metro[235]=375&metro[236]=376&metro[237]=377&metro[238]=378&metro[239]=379&metro[23]=24&metro[240]=380&metro[241]=384&metro[242]=385&metro[243]=386&metro[244]=387&metro[245]=388&metro[246]=389&metro[247]=390&metro[248]=391&metro[249]=392&metro[24]=25&metro[250]=393&metro[251]=394&metro[252]=395&metro[253]=396&metro[254]=398&metro[255]=399&metro[256]=400&metro[257]=423&metro[258]=424&metro[259]=425&metro[25]=26&metro[260]=426&metro[261]=427&metro[262]=428&metro[263]=429&metro[264]=430&metro[265]=439&metro[266]=440&metro[267]=441&metro[268]=443&metro[269]=444&metro[26]=27&metro[270]=445&metro[271]=446&metro[272]=447&metro[273]=448&metro[274]=449&metro[275]=450&metro[276]=451&metro[277]=452&metro[278]=453&metro[279]=454&metro[27]=28&metro[280]=463&metro[281]=466&metro[282]=467&metro[283]=470&metro[284]=472&metro[285]=473&metro[286]=477&metro[287]=507&metro[288]=509&metro[289]=511&metro[28]=29&metro[290]=512&metro[291]=513&metro[292]=514&metro[293]=515&metro[294]=516&metro[295]=517&metro[296]=518&metro[297]=519&metro[298]=520&metro[299]=521&metro[29]=30&metro[2]=3&metro[300]=524&metro[301]=533&metro[302]=534&metro[303]=535&metro[304]=536&metro[305]=537&metro[306]=538&metro[307]=539&metro[308]=540&metro[309]=541&metro[30]=31&metro[310]=542&metro[311]=546&metro[312]=548&metro[313]=549&metro[314]=554&metro[315]=555&metro[316]=556&metro[317]=558&metro[318]=559&metro[319]=560&metro[31]=32&metro[320]=561&metro[321]=562&metro[322]=563&metro[323]=564&metro[324]=565&metro[325]=566&metro[326]=567&metro[327]=568&metro[328]=569&metro[329]=570&metro[32]=33&metro[330]=571&metro[331]=572&metro[332]=573&metro[333]=574&metro[33]=34&metro[34]=35&metro[35]=36&metro[36]=37&metro[37]=38&metro[38]=39&metro[39]=40&metro[3]=4&metro[40]=41&metro[41]=42&metro[42]=43&metro[43]=44&metro[44]=45&metro[45]=46&metro[46]=47&metro[47]=48&metro[48]=49&metro[49]=50&metro[4]=5&metro[50]=51&metro[51]=52&metro[52]=53&metro[53]=54&metro[54]=55&metro[55]=56&metro[56]=57&metro[57]=58&metro[58]=59&metro[59]=60&metro[5]=6&metro[60]=61&metro[61]=62&metro[62]=63&metro[63]=64&metro[64]=65&metro[65]=66&metro[66]=67&metro[67]=68&metro[68]=69&metro[69]=70&metro[6]=7&metro[70]=71&metro[71]=72&metro[72]=73&metro[73]=74&metro[74]=75&metro[75]=76&metro[76]=77&metro[77]=78&metro[78]=79&metro[79]=80&metro[7]=8&metro[80]=81&metro[81]=82&metro[82]=83&metro[83]=84&metro[84]=85&metro[85]=86&metro[86]=87&metro[87]=88&metro[88]=89&metro[89]=90&metro[8]=9&metro[90]=91&metro[91]=92&metro[92]=93&metro[93]=94&metro[94]=95&metro[95]=96&metro[96]=97&metro[97]=98&metro[98]=99&metro[99]=100&metro[9]=10&";
        String resaleProperty = "object_type[0]=1&"; // ВТОРИЧКА (ЕСЛИ 0 ТО И НОВОСТРОЙКИ)
        String offerType = "offer_type=flat&"; // КВАРТИРА
        String onlyFlat = "only_flat=1&"; // НЕ АППАРТАМЕНТЫ
//        String noRepair = "repair[0]=1&"; // БЕЗ РЕМОНТА
//        String cosmeticRepair = "repair[1]=2&"; // КОСМЕТИЧЕСКИЙ
        String room1 = "room1=1&"; // ОДНУШКА
        String room2 = "room2=1&"; // ДВУШКА
        String room3 = "room3=1&"; //ТРЕШКА
        String studio = "room9=1&"; // СТУДИЯ
        String page = "p=" + pageNum + "&"; // НОМЕР СТРАНИЦЫ
        String sort = "sort=creation_date_desc"; // СОТРИРОВКА ПО ДАТЕ

        String finalUrl = url + currency + engineVersion + dealType + demolishedInMoscowProgramm + electronicTrading + flatShare + m2MaxPrice + isFirstFloor + maxtarea + footMin + onlyFoot + metro + resaleProperty +
                offerType + onlyFlat +
//                noRepair + cosmeticRepair +
                room1 + room2 + room3 + studio + page + sort;

        log.info("Request created {}", finalUrl);

        return finalUrl;
    }

    public String sendRequest(HttpRequest request) {
        int retryCount = 0;
        int statusCode = 0;
        HttpResponse<String> response = null;
        do {
            if (retryCount > 7) {
                log.error("Cian is not responding for {}", request.uri());
                return null;
            }
            try (HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build()) {
                Thread.sleep(retryCount > 4 ? 60000 : (retryCount > 2 ? 15000 : 5000));
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                statusCode = response.statusCode();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            retryCount++;
        } while (statusCode != 200 && statusCode != 204);

        return response.body();
    }

    public Set<String> parsePageWithAllFlats(String response) {

        Document doc = Jsoup.parse(response);
        Elements scripts = doc.select("script");

        String htmlElement = "";
        for (Element script : scripts) {
            if (script.html().contains("window._cianConfig['frontend-serp']")) {
                htmlElement = script.html();
            }
        }
        htmlElement = htmlElement.replace("window._cianConfig = window._cianConfig || {};\n" +
                "window._cianConfig['frontend-serp'] = (window._cianConfig['frontend-serp'] || []).concat([", "{\"test\":[");
        htmlElement = htmlElement.replace("]);", "]}");

        JSONObject jsonObject = new JSONObject(htmlElement);

        JSONArray jsonArray = jsonObject.getJSONArray("test");

        JSONArray offers = null;
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.getJSONObject(i).has("key") && jsonArray.getJSONObject(i).getString("key").equals("initialState")) {
                offers = jsonArray.getJSONObject(i).getJSONObject("value").getJSONObject("results").getJSONArray("offers");
                break;
            }
        }
        Set<String> cianIds = new HashSet<>();
        for (int i = 0; i < Objects.requireNonNull(offers).length(); i++) {
            cianIds.add(offers.getJSONObject(i).get("cianId").toString());
        }
        log.info("Cian Ids on page {}", cianIds);

        return cianIds;

    }

    public boolean parseFlat(String cianId) {
        log.info("Start parsing flat {}", cianId);

        if (flatRepository.existsByCianId(Integer.parseInt(cianId))) {
            return false;
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cian.ru/price-estimator/v1/get-estimation-and-trend-web/?cianOfferId=" + cianId))
                .GET()
                .build();
        String response = sendRequest(request);
        if (response == null) {
            return true;
        }

        JSONObject jsonPrice = new JSONObject(response);
        String lowestPrice;
        String currentPrice;
// проверить offerPricePercentage тег в json
        try {
            lowestPrice = jsonPrice.getJSONObject("priceInfo").getJSONObject("priceTag").getString("estimationLowerBoundShort").replace(",", ".");
            currentPrice = jsonPrice.getJSONObject("priceInfo").getJSONObject("priceTag").getString("offerPriceShort").replace(",", ".");
        } catch (JSONException e) {
            priceParsingErrorCianIdsMap.merge(cianId, 1, Integer::sum);
            if (priceParsingErrorCianIdsMap.get(cianId) > 2) {
                log.error("Error parsing price in flat {}", cianId);
                priceParsingErrorCianIdsMap.remove(cianId);
            } else {
                analyzedCianIds.remove(cianId);
            }
            return true;
        }

        priceParsingErrorCianIdsMap.remove(cianId);

        double lowestPriceDouble = Double.parseDouble(lowestPrice);
        double currentPriceDouble = Double.parseDouble(currentPrice);
        if (lowestPriceDouble * 0.95 > currentPriceDouble) {
            finalCianIds.add(cianId);
            log.info("{} is good", cianId);

            try {
                Flat f = new Flat();
                f.setCianId(Integer.parseInt(cianId));
                f.setLowCianPrice((int) (lowestPriceDouble * 1000000));
                f.setPriceGap(100 - (currentPriceDouble * 100 / lowestPriceDouble));

                JSONArray jsonArray = getJSONArrayForStats(cianId);

                f.setPrice(getPrice(jsonArray));
                f.setViewsCount(getViewsCount(jsonArray));
                f.setPriceM2(getPriceM2(jsonArray));
                f.setAddress(getAddress(jsonArray));
                f.setMetro(getMetro(jsonArray));
                f.setMetroMinWalkTime(getMetroMinWalkTime(jsonArray));
                f.setDistrict(getDistrict(jsonArray));
                f.setFloor(getFloor(jsonArray));
                f.setBuildingFloors(getBuildingFloorsCount(jsonArray));
                f.setIsFirstFloor(f.getFloor() == 1);
                f.setIsLastFloor(f.getFloor() == f.getBuildingFloors());
                f.setM2(getM2(jsonArray));
                f.setRooms(getRooms(jsonArray));
                f.setTime(LocalDateTime.now());
                f.setNearbyFlatsMessage(getNearbyFlatsMessage(cianId, f.getPrice(), f.getRooms(), f.getM2()));

                flatRepository.save(f);
            } catch (JSONException e) {
                log.error("Error parsing flat {}", cianId);
                log.error(e.getMessage(), e);
                return true;
            }
        }
        return true;
    }

    private int getPrice(JSONArray jsonArray) {
        JSONObject bargainTerms = null;
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.getJSONObject(i).has("key") && jsonArray.getJSONObject(i).getString("key").equals("defaultState")) {
                bargainTerms = jsonArray.getJSONObject(i).getJSONObject("value").getJSONObject("offerData").getJSONObject("offer").getJSONObject("bargainTerms");
                break;
            }
        }
        String price = Objects.requireNonNull(bargainTerms).get("price").toString();

        return Integer.parseInt(price);
    }


    private JSONArray getJSONArrayForStats(String cianId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.cian.ru/sale/flat/" + cianId))
                .GET()
                .build();
        String response = sendRequest(request);

        Document doc = Jsoup.parse(response);
        Elements scripts = doc.select("script");

        String htmlElement = "";
        for (Element script : scripts) {
            if (script.html().contains("window._cianConfig['frontend-offer-card']")) {
                htmlElement = script.html();
            }
        }
        htmlElement = htmlElement.replace("window._cianConfig = window._cianConfig || {};\n" +
                "window._cianConfig['frontend-offer-card'] = (window._cianConfig['frontend-offer-card'] || []).concat([", "{\"test\":[");
        htmlElement = htmlElement.replace("]);", "]}");

        JSONObject jsonObject = new JSONObject(htmlElement);

        return jsonObject.getJSONArray("test");
    }

    private int getViewsCount(JSONArray jsonArray) {
        JSONObject offerData;
        JSONObject stats = null;

        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.getJSONObject(i).has("key") && jsonArray.getJSONObject(i).getString("key").equals("defaultState")) {
                offerData = jsonArray.getJSONObject(i).getJSONObject("value").getJSONObject("offerData");
                try {
                    stats = offerData.getJSONObject("stats");
                } catch (JSONException e) {
                    return 0;
                }
                break;
            }
        }
        String totalStats = Objects.requireNonNull(stats).get("total").toString();

        return Integer.parseInt(totalStats);
    }

    private int getPriceM2(JSONArray jsonArray) {
        JSONObject priceInfo = null;

        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.getJSONObject(i).has("key") && jsonArray.getJSONObject(i).getString("key").equals("defaultState")) {
                priceInfo = jsonArray.getJSONObject(i).getJSONObject("value").getJSONObject("offerData").getJSONObject("priceInfo");
                break;
            }
        }
        String pricePerSquareValue = Objects.requireNonNull(priceInfo).get("pricePerSquareValue").toString();

        return Integer.parseInt(pricePerSquareValue);
    }

    private String getAddress(JSONArray jsonArray) {
        JSONArray address = getAddressNonProcessed(jsonArray);

        StringBuilder resAddress = new StringBuilder();
        for (int i = 0; i < Objects.requireNonNull(address).length(); i++) {
            resAddress.append(address.getJSONObject(i).get("fullName").toString()).append(" ");
        }

        return resAddress.toString();
    }

    private String getDistrict(JSONArray jsonArray) {
        JSONArray address = getAddressNonProcessed(jsonArray);

        String district = "";
        for (int i = 0; i < Objects.requireNonNull(address).length(); i++) {
            if (address.getJSONObject(i).get("type").toString().equals("okrug")) {
                district = address.getJSONObject(i).get("fullName").toString();
                break;
            }
        }

        if (district.equals("НАО (Новомосковский)")) {
            district = "НАО";
        }

        return district;
    }

    private JSONArray getAddressNonProcessed(JSONArray jsonArray) {
        JSONArray address = null;
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.getJSONObject(i).has("key") && jsonArray.getJSONObject(i).getString("key").equals("defaultState")) {
                address = jsonArray.getJSONObject(i).getJSONObject("value").getJSONObject("offerData").getJSONObject("offer").getJSONObject("geo").getJSONArray("address");
                break;
            }
        }
        return address;
    }

    private String getMetro(JSONArray jsonArray) {
        JSONArray metro = getMetroNonProcessed(jsonArray);
        StringBuilder resMetro = new StringBuilder();
        for (int i = 0; i < Objects.requireNonNull(metro).length(); i++) {
            if (metro.getJSONObject(i).get("travelType").toString().equals("walk") && !resMetro.toString().contains(metro.getJSONObject(i).get("name").toString())) {
                resMetro.append(metro.getJSONObject(i).get("name").toString()).append("(").append(metro.getJSONObject(i).get("travelTime").toString()).append(" минут пешком)").append(" ");
            } else if (metro.getJSONObject(i).get("travelType").toString().equals("transport") && !resMetro.toString().contains(metro.getJSONObject(i).get("name").toString())) {
                resMetro.append(metro.getJSONObject(i).get("name").toString()).append("(").append(metro.getJSONObject(i).get("travelTime").toString()).append(" минут на авто)").append(" ");
            }
        }

        return resMetro.toString();
    }

    private Short getMetroMinWalkTime(JSONArray jsonArray) {
        JSONArray metro = getMetroNonProcessed(jsonArray);

        short metroMinWalkTime = 100;
        short currentStationWalkTime;
        for (int i = 0; i < Objects.requireNonNull(metro).length(); i++) {
            if (metro.getJSONObject(i).get("travelType").toString().equals("walk")) {
                if (metro.getJSONObject(i).get("travelTime").toString() != null) {
                    currentStationWalkTime = Short.parseShort(metro.getJSONObject(i).get("travelTime").toString());
                    if (currentStationWalkTime < metroMinWalkTime) {
                        metroMinWalkTime = currentStationWalkTime;
                    }
                }
            }
        }

        return metroMinWalkTime == 100 ? null : metroMinWalkTime;
    }

    private JSONArray getMetroNonProcessed(JSONArray jsonArray) {
        JSONArray metro = null;
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.getJSONObject(i).has("key") && jsonArray.getJSONObject(i).getString("key").equals("defaultState")) {
                metro = jsonArray.getJSONObject(i).getJSONObject("value").getJSONObject("offerData").getJSONObject("offer").getJSONObject("geo").getJSONArray("undergrounds");
                break;
            }
        }
        return metro;
    }

    private short getFloor(JSONArray jsonArray) {
        JSONObject offer = getOfferNonProcessed(jsonArray);

        String floor = offer.get("floorNumber").toString();

        return Short.parseShort(floor);
    }

    private int getBuildingFloorsCount(JSONArray jsonArray) {
        JSONObject offer = getOfferNonProcessed(jsonArray);

        String floor = offer.getJSONObject("building").get("floorsCount").toString();

        return Integer.parseInt(floor);
    }

    private short getM2(JSONArray jsonArray) {
        JSONObject offer = getOfferNonProcessed(jsonArray);

        String m2 = offer.get("totalArea").toString();

        return (short) Double.parseDouble(m2);
    }

    private short getRooms(JSONArray jsonArray) {
        JSONObject offer = getOfferNonProcessed(jsonArray);

        String rooms = offer.optString("roomsCount", "0");

        return Short.parseShort(rooms);
    }

    private JSONObject getOfferNonProcessed(JSONArray jsonArray) {
        JSONObject offer = null;
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.getJSONObject(i).has("key") && jsonArray.getJSONObject(i).getString("key").equals("defaultState")) {
                offer = jsonArray.getJSONObject(i).getJSONObject("value").getJSONObject("offerData").getJSONObject("offer");
                break;
            }
        }

        return offer;
    }

    private String getNearbyFlatsMessage(String cianId, int price, short rooms, short m2) {
        int minPrice = Integer.MAX_VALUE;
        int maxPrice = 0;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cian.ru/search-engine/v3/get-similar-offers/"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"cianOfferId\":" + cianId + "}"))
                .build();
        String response = sendRequest(request);
        if (response == null) {
            return "";
        }
        JSONObject jsonObject = new JSONObject(response);
        JSONArray offers = jsonObject.getJSONArray("offers");
        String[] title;
        Matcher matcher;
        Pattern pattern = Pattern.compile("\\d+");
        short offeredFlatM2;
        StringBuilder resultMessage = new StringBuilder();
        Map<String, Integer> offeredFlats = new HashMap<>();
        for (int i = 0; i < offers.length(); i++) {
            if (price < offers.getJSONObject(i).getDouble("totalPriceRur")) {
                title = offers.getJSONObject(i).getString("title").split(",");
                matcher = pattern.matcher(title[0]);
                if (matcher.find()) {
                    if (rooms == Short.parseShort(matcher.group())) {
                        matcher = pattern.matcher(title[1]);
                        if (matcher.find()) {
                            offeredFlatM2 = Short.parseShort(matcher.group());
                            if (m2 > offeredFlatM2 * 0.8 && m2 < offeredFlatM2 * 1.2) {
                                minPrice = (int) Math.min(minPrice, offers.getJSONObject(i).getDouble("totalPriceRur"));
                                maxPrice = (int) Math.max(maxPrice, offers.getJSONObject(i).getDouble("totalPriceRur"));

                                offeredFlats.put("https://www.cian.ru" + offers.getJSONObject(i).getString("url") + "\n", (int) offers.getJSONObject(i).getDouble("totalPriceRur"));
                            }
                        }
                    }
                }

            }

        }
        if (!offeredFlats.isEmpty()) {
            offeredFlats.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(stringIntegerEntry -> resultMessage.append(stringIntegerEntry.getKey()));
        }
        if (!resultMessage.isEmpty()) {
            String prefix = "‼️Конкуренты‼️\n" + "\n" + "Диапазон цен: " + formatter.format(minPrice) + " *₽* - " + formatter.format(maxPrice) + " *₽*" + "\n\n";
            resultMessage.insert(0, prefix);
            return resultMessage.toString();
        }
        return "";
    }
}

