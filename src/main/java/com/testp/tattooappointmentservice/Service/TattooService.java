package com.testp.tattooappointmentservice.Service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testp.tattooappointmentservice.Enums.TattooStyle;
import com.testp.tattooappointmentservice.Models.TattooAppointment;
import com.testp.tattooappointmentservice.Models.TattooData;
import com.testp.tattooappointmentservice.Repository.TattooRepo;
import com.testp.tattooappointmentservice.Requests.GetPriceQuoteForCustomTattooReq;
import com.testp.tattooappointmentservice.Requests.GetPriceQuoteReq;
import com.testp.tattooappointmentservice.Requests.SendMailReq;
import com.testp.tattooappointmentservice.Requests.TattooAppointmentReq;
import com.testp.tattooappointmentservice.Responses.PriceQuoteForCustomTattooRes;
import com.testp.tattooappointmentservice.Responses.PriceQuoteRes;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TattooService {
    private final OpenAIClient client;
    private final TattooRepo repo;
    private final MailService mailService;

    public Object saveTattooAppointment(TattooAppointmentReq req) throws MessagingException, IOException {
        List<String> bodyParts = req.getTattooData().stream().map(TattooData::getBodyPart).toList();
        List<Integer> prices = req.getTattooData().stream().map(TattooData::getTattooPrice).toList();
        List<Double> widths = req.getTattooData().stream().map(TattooData::getWidth).toList();
        List<Double> heights = req.getTattooData().stream().map(TattooData::getHeight).toList();
        List<List<MultipartFile>> refs = req.getTattooData().stream().map(TattooData::getTattooRefferences).toList();
        List<Boolean> isLargeTattoo = req.getTattooData().stream().map(TattooData::getLargeTattoo).toList();
        List<Boolean> isCustomDesignTattoo = req.getTattooData().stream().map(TattooData::getCustomDesignTattoo).toList();
        List<String> customDesignTattooTexts = req.getTattooData().stream().map(TattooData::getCustomDesignTattooText).toList();

        TattooAppointment appointment = TattooAppointment.builder()
                .email(req.getEmail())
                .phoneNumber(req.getPhoneNumber())
                .lastName(req.getLastName())
                .firstName(req.getFirstName())
                .style(TattooStyle.black)
                .bodyParts(bodyParts)
                .tattooPrice(prices)
                .build();
        repo.save(appointment);
        SendMailReq smr = SendMailReq.builder()
                .appId(appointment.getAppId())
                .bodyParts(bodyParts)
                .lastName(req.getLastName())
                .firstName(req.getFirstName())
                .price(prices)
                .width(widths)
                .height(heights)
                .tattooRefference(refs)
                .phoneNumber(req.getPhoneNumber())
                .email(req.getEmail())
                .largeTattoo(isLargeTattoo)
                .customDesignTattoo(isCustomDesignTattoo)
                .customDesignTattooText(customDesignTattooTexts)
                .build();

        mailService.sendMeMail(smr);
        mailService.sendClientMail(smr);
        return "CREATED";
    }

    public PriceQuoteRes getPriceQuote(GetPriceQuoteReq req) throws JsonProcessingException {
        Double width = null;
        Double height = null;
        Double area = null;
        if(req.getSizeHeight() != null && req.getSizeWidth() != null){
            width = req.getSizeWidth();
            height = req.getSizeHeight();
            area = width * height;
        }
        //String sizeCode = sizeCodeFromArea(area);

        String prompt = buildNormalPrompt(height, width, area);

        MultipartFile tattooRefference = req.getTattooRefference();
        String base64Image =  toBase64(tattooRefference);

        ObjectMapper mapper = new ObjectMapper();

        ChatRequestUserMessage userMessage = new ChatRequestUserMessage(
                Arrays.asList(
                        new ChatMessageImageContentItem(new ChatMessageImageUrl(base64Image)),
                        new ChatMessageTextContentItem(prompt)
                )
        );
        ChatCompletionsOptions options = new ChatCompletionsOptions(
                Arrays.asList(
                        new ChatRequestSystemMessage("You are a tattoo pricing assistant."),
                        userMessage
                )
        );
        ChatCompletions completions =
                client.getChatCompletions("gpt-5.4-mini", options);

        String content = completions.getChoices()
                .getFirst()
                .getMessage()
                .getContent();

        return mapper.readValue(content, PriceQuoteRes.class);
    }
    private  String sizeCodeFromArea(double area) {
        if (area <= 25) return "XS";
        if (area <= 75) return "S";
        if (area <= 150) return "M";
        if (area <= 250) return "L";
        if (area <= 400) return "XL";
        if (area <= 600) return "XXL";
        return "Egyedi";
    }
    private String toBase64(MultipartFile file) {
        try {
            return Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Kép konvertálási hiba", e);
        }
    }

    private String buildNormalPrompt(Double height, Double width, Double area) {
        var finalWidth = width != null && width != 0 ? width.toString() + " cm" : "Nincs megadva, meg kell becsülni";
        var finalHeight = height != null && height != 0 ? height.toString() + " cm" : "Nincs megadva, meg kell becsülni";
        var finalArea = width != null && width != 0  && height != null && height != 0  ? height*width : "Nincs megadva, ki kell számolni!";
        return """
                Elemezd a tetoválás fényképét és a vendég által megadott méreteket (cm), amennyiben megadta, ha nem adta meg, amikor ez a szöveg van a méretadatok mellett: 'Nincs megadva, meg kell becsülni', akkor azt pontosan becsüld meg a kép alapján, és azzal dolgozz.
                
                              A feladatod: a tetoválás kategorizálása és az ár kiszámítása a következő árlista alapján.
                              A számítás során kizárólag a megadott árlistát használhatod.
                              HA BECSÜLNÖD KELLETT A MÉRETEKET, ÉS íGY A TERÜLET NAGYOBB MINT 600 cm² AKKOR NE KATEGORIZÁLJ, HANEM UGORJ A VÉGÉRE ÉS RAKD ÖSSZE A JSON VÁLASZT, MELYBEN A 'price' ÉRTÉKE LEGYEN 0!
                
                              Méretadatok:
                               - magasság: %s
                               - szélesség: %s
                               - terület: %s
                              FONTOS:
                              A tetoválás körbefutó kategóriába akkor sorolandó, ha a képen látható minta:
                              - teljesen körbefutja a kart, lábszárat vagy combot,
                              - pánt jellegű,
                              - 360°‑ban záródó mintát alkot
                              Ha a tetoválás körbefutó jellegű, akkor a méretkód: Pánt.
                              Ebben az esetben a felület szerinti méretkódot figyelmen kívül kell hagyni.
                
                              Árlista:
                
                              XS (Apró, ≤25 cm²)
                              - Normál: 8 000 Ft
                              - Normál (csak szöveg): 8 000 Ft
                              - Normál + tervezés: 10 000 Ft
                              - Állat: 10 000 Ft
                              - Részletes: 10 000 Ft
                              - Részletes + tervezés: 15 000 Ft
                
                              S (Kicsi, 26–75 cm²)
                              - Normál: 13 000 Ft
                              - Normál (csak szöveg): 10 000 Ft
                              - Normál + tervezés: 18 000 Ft
                              - Állat: 18 000 Ft
                              - Részletes: 18 000 Ft
                              - Részletes + tervezés: 23 000 Ft
                
                              M (Közepes, 76–150 cm²)
                              - Normál: 20 000 Ft
                              - Normál (csak szöveg): 18 000 Ft
                              - Normál + tervezés: 30 000 Ft
                              - Állat: 30 000 Ft
                              - Részletes: 30 000 Ft
                              - Részletes + tervezés: 35 000 Ft
                
                              L (Közepes/Nagy, 151–250 cm²)
                              - Normál: 40 000 Ft
                              - Normál (csak szöveg): 20 000 Ft
                              - Normál + tervezés: 45 000 Ft
                              - Állat: 45 000 Ft
                              - Részletes: 45 000 Ft
                              - Részletes + tervezés: 50 000 Ft
                
                              XL (Nagy, 251–400 cm²)
                              - Normál: 45 000 Ft
                              - Normál (csak szöveg): 25 000 Ft
                              - Normál + tervezés: 50 000 Ft
                              - Állat: 50 000 Ft
                              - Részletes: 50 000 Ft
                              - Részletes + tervezés: 55 000 Ft
                
                              XXL (Nagy+, 401–600 cm²)
                              - Normál: 50 000 Ft
                              - Normál (csak szöveg): 30 000 Ft
                              - Normál + tervezés: 55 000 Ft
                              - Állat: 55 000 Ft
                              - Részletes: 55 000 Ft
                              - Részletes + tervezés: 60 000 Ft
                
                              Pánt (Körbefutó – Alkar / Lábszár / Comb)
                              - Normál: 45 000 Ft
                              - Normál (csak szöveg): -
                              - Normál + tervezés: 50 000 Ft
                              - Állat: -
                              - Részletes: 60 000 Ft
                              - Részletes + tervezés: 70 000 Ft
                
                              Egyedi (>600 cm²)
                              - Minden kategória: Egyedi árazás
                
                
                              Feladat:
                
                              1. Elemezd a tetoválás fényképét.
                
                              2. Állapítsd meg:
                                 - complexity: egyszerű / közepes / részletes
                                 - designNeeded = true csak akkor, ha a tetoválás elkészítéséhez
                              	a tetoválónak új grafikát kell létrehoznia.
                              	false:
                              	- Pinterest referencia alapján készült egyszerű minta
                              	- egyszerű szimbólum
                              	- geometria
                              	- alap állat kontúr
                              	- egyszerű szöveg
                              	- kész sablon jellegű minta
                
                                   	true:
                                   	- kliens egyedi ötlete alapján készülő rajz
                                   	- személyre szabott karakter
                                   	- egyedi kompozíció
                                   	- több elem összeállítása
                                  	- művészi újratervezést igényel
                                 - isAnimal: true/false
                                 - isText: true/false
                                 - isWrapAround: true/false  ← Ezt TE állapítod meg a kép alapján.
                
                              3. Ha isWrapAround = true → méretkód = Pánt.
                                 Ha isWrapAround = false → méretkód = felület alapján.
                
                              4. A kategóriák közül válaszd ki:
                                 - normal
                                 - normal_text
                                 - normal_custom
                                 - animal
                                 - detailed
                                 - detailed_custom
                
                              5. A táblázat alapján számold ki a végső árat.
                
                              6. Add vissza a következő JSON-t:
                
                              {
                                "estimatedWidth": amennyiben meg kellett becsülni, akkor a becsült érték kerül ide, különben null;
                                "estimatedHeight": amennyiben meg kellett becsülni, akkor a becsült érték kerül ide, különben null;
                                "isWrapAround": true/false,
                                "complexity": "...",
                                "designNeeded": true/false,
                                "isAnimal": true/false,
                                "isText": true/false,
                                "category": "...",
                                "price": number,
                                "reason": "rövid szöveges magyarázat"
                              }
                
                              FONTOS:
                              A választ kizárólag érvényes JSON formátumban add vissza.
                
                              Tilos:
                              - markdown
                              - magyarázó szöveg JSON-on kívül
                              - komment
                              - ``` karakterek
                
                              A válasz mindig ugyanazt a struktúrát használja.
                """.formatted(finalHeight, finalWidth, finalArea);
    }

    private String buildCustomPrompt(String customText, Double width, Double height) {
        var tattooWidth = width != null && width != 0 ? width.toString() + " cm" : "Nincs megadva, meg kell becsülni";
        var tattooHeight = height != null && height != 0 ? height.toString() + " cm" : "Nincs megadva, meg kell becsülni";
        return """
                Az ügyfél egy teljesen egyedileg tervezett tetkót szeretne, ennek érdekében képet/képeket is feltöltött majd írt egy megjegyzést, hogy hogyan tervezte el a designt!
                Amennyiben az ügyfél megjegyzése eltér a témától, irrealisztikus, vagy bármi ilyesmi, amit nem lehet teljesjteni, akkor add vissza a prompt végén lévő JSON-t úgy,
                hogy a reason mezőjébe beleírod, hogy nem megfelelő leírás, a többi mezőt pedig nullra állítod!
                
                
                Elemezd a tetoválás képet/képeket és becsüld meg a tetoválás magasságát és szélességét (ha nem adta meg az ügyfél) az ügyfél megjegyzése és a képek alapján!
                
                A feladatod: a tetoválás kategorizálása és az ár kiszámítása a következő árlista alapján.
                
                Amennyiben szükséges, akkor az általad reálisan becsült méretekkel (szélesség, magasság) - az ügyfél megjegyzése alapján - számold ki a területet, majd ez alapján kategorizálj.
                
                HA BECSÜLNÖD KELLETT A MÉRETEKET, ÉS íGY A TERÜLET NAGYOBB MINT 600 cm² AKKOR NE KATEGORIZÁLJ, HANEM UGORJ A VÉGÉRE ÉS RAKD ÖSSZE A JSON VÁLASZT, MELYBEN A 'price' ÉRTÉKE LEGYEN 0!
                
                A számítás során kizárólag a megadott árlistát használhatod.
               
                Adatok:
                -magasság: %s
                -szélesség: %s
                
                Az ügyfél megjegyzése:
                %s
              
                
                FONTOS:
                A tetoválás körbefutó kategóriába akkor sorolandó, ha a képen látható minta:
                - teljesen körbefutja a kart, lábszárat vagy combot,
                - pánt jellegű,
                - 360°‑ban záródó mintát alkot
                
                Ha a tetoválás körbefutó jellegű, akkor a méretkód: Pánt.
                Ebben az esetben a felület szerinti méretkódot figyelmen kívül kell hagyni.
                
                Árlista:
                
                XS (Apró, ≤25 cm²)
                - Normál: 8 000 Ft
                - Normál (csak szöveg): 8 000 Ft
                - Normál + tervezés: 10 000 Ft
                - Állat: 10 000 Ft
                - Részletes: 10 000 Ft
                - Részletes + tervezés: 15 000 Ft
                
                S (Kicsi, 26–75 cm²)
                - Normál: 13 000 Ft
                - Normál (csak szöveg): 10 000 Ft
                - Normál + tervezés: 18 000 Ft
                - Állat: 18 000 Ft
                - Részletes: 18 000 Ft
                - Részletes + tervezés: 23 000 Ft
                
                M (Közepes, 76–150 cm²)
                - Normál: 20 000 Ft
                - Normál (csak szöveg): 18 000 Ft
                - Normál + tervezés: 30 000 Ft
                - Állat: 30 000 Ft
                - Részletes: 30 000 Ft
                - Részletes + tervezés: 35 000 Ft
                
                L (Közepes/Nagy, 151–250 cm²)
                - Normál: 40 000 Ft
                - Normál (csak szöveg): 20 000 Ft
                - Normál + tervezés: 45 000 Ft
                - Állat: 45 000 Ft
                - Részletes: 45 000 Ft
                - Részletes + tervezés: 50 000 Ft
                
                XL (Nagy, 251–400 cm²)
                - Normál: 45 000 Ft
                - Normál (csak szöveg): 25 000 Ft
                - Normál + tervezés: 50 000 Ft
                - Állat: 50 000 Ft
                - Részletes: 50 000 Ft
                - Részletes + tervezés: 55 000 Ft
                
                XXL (Nagy+, 401–600 cm²)
                - Normál: 50 000 Ft
                - Normál (csak szöveg): 30 000 Ft
                - Normál + tervezés: 55 000 Ft
                - Állat: 55 000 Ft
                - Részletes: 55 000 Ft
                - Részletes + tervezés: 60 000 Ft
                
                Pánt (Körbefutó – Alkar / Lábszár / Comb)
                - Normál: 45 000 Ft
                - Normál (csak szöveg): -
                - Normál + tervezés: 50 000 Ft
                - Állat: -
                - Részletes: 60 000 Ft
                - Részletes + tervezés: 70 000 Ft
                
                Egyedi (>600 cm²)
                - Minden kategória: Egyedi árazás
                Í
                Feladat:
                
                1. Elemezd a képet/képeket.
                
                2. Állapítsd meg:
                   - complexity: egyszerű / közepes / részletes
                   - designNeeded = true csak akkor, ha a tetoválás elkészítéséhez
                	a tetoválónak új grafikát kell létrehoznia.
                	false:
                	- Pinterest referencia alapján készült egyszerű minta
                	- egyszerű szimbólum
                	- geometria
                	- alap állat kontúr
                	- egyszerű szöveg
                	- kész sablon jellegű minta
                
                     	true:
                     	- kliens egyedi ötlete alapján készülő rajz
                     	- személyre szabott karakter
                     	- egyedi kompozíció
                     	- több elem összeállítása
                    	- művészi újratervezést igényel
                   - isAnimal: true/false
                   - isText: true/false
                   - isWrapAround: true/false  ← Ezt TE állapítod meg a kép alapján.
                
                3. Ha isWrapAround = true → méretkód = Pánt.
                   Ha isWrapAround = false → méretkód = felület alapján.
                
                4. A kategóriák közül válaszd ki:
                   - normal
                   - normal_text
                   - normal_custom
                   - animal
                   - detailed
                   - detailed_custom
                
                5. A táblázat alapján számold ki a végső árat.
                
                6. Add vissza a következő JSON-t:
                
                {
                  "estimatedWidth": number,
                  "estimatedHeight": number,
                  "isWrapAround": true/false,
                  "complexity": "...",
                  "designNeeded": true/false,
                  "isAnimal": true/false,
                  "isText": true/false,
                  "category": "...",
                  "price": number,
                  "reason": "rövid szöveges magyarázat"
                }
                
                FONTOS:
                A választ kizárólag érvényes JSON formátumban add vissza.
                
                Tilos:
                - markdown
                - magyarázó szöveg JSON-on kívül
                - komment
                - ``` karakterek
                
                A válasz mindig ugyanazt a struktúrát használja.
                
                """.formatted(customText,tattooHeight, tattooHeight);
    }

    public PriceQuoteForCustomTattooRes getPriceQuoteForCustomTattoo(GetPriceQuoteForCustomTattooReq req) throws JsonProcessingException {
        String prompt = buildCustomPrompt(req.getCustomTattooText(), req.getWidth(), req.getHeight());
        List<MultipartFile> ref = req.getTattooRefference();
        List<String> base64Refs = new ArrayList<>(ref.size());
        for (MultipartFile file : ref) {
            base64Refs.add(toBase64(file));
        }
        List<ChatMessageContentItem> contentItems = new ArrayList<>(base64Refs.size());
        for (String base64Ref : base64Refs) {
            contentItems.add(
                    new ChatMessageImageContentItem(
                            new ChatMessageImageUrl(base64Ref)
                    ));
        }
        contentItems.add(new ChatMessageTextContentItem(prompt));

        ObjectMapper objectMapper = new ObjectMapper();

        ChatRequestUserMessage message = new ChatRequestUserMessage(
               contentItems
        );
        ChatCompletionsOptions options = new ChatCompletionsOptions(
                Arrays.asList(
                        new ChatRequestSystemMessage("You are a tattoo pricing assistant."),
                        message
                )
        );
        ChatCompletions completions = client.getChatCompletions("gpt-5.4-mini", options);

        String content = completions.getChoices().getFirst().getMessage().getContent();

        return objectMapper.readValue(content, PriceQuoteForCustomTattooRes.class);
    }
}
