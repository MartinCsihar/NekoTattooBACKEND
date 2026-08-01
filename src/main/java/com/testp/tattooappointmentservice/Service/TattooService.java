package com.testp.tattooappointmentservice.Service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testp.tattooappointmentservice.Models.TattooAppointment;
import com.testp.tattooappointmentservice.Repository.TattooRepo;
import com.testp.tattooappointmentservice.Requests.GetPriceQuoteReq;
import com.testp.tattooappointmentservice.Requests.SendMailReq;
import com.testp.tattooappointmentservice.Requests.TattooAppointmentReq;
import com.testp.tattooappointmentservice.Responses.PriceQuoteRes;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class TattooService {
    private final OpenAIClient client;
    private final TattooRepo repo;
    private final MailService mailService;

    public Object saveTattooAppointment(TattooAppointmentReq req) throws MessagingException, IOException {
        TattooAppointment appointment = TattooAppointment.builder()
                .email(req.getEmail())
                .phoneNumber(req.getPhoneNumber())
                .lastName(req.getLastName())
                .firstName(req.getFirstName())
                .appointmentDate(req.getAppointmentDate())
                .appointmentTime(req.getAppointmentTime())
                .tattooHeight(req.getTattooHeight())
                .tattooWidth(req.getTattooWidth())
                .style(req.getStyle())
                .tattooPrice(req.getTattooPrice())
                .build();
        repo.save(appointment);
        SendMailReq smr = SendMailReq.builder()
                .appId(appointment.getAppId())
                .lastName(req.getLastName())
                .firstName(req.getFirstName())
                .price(req.getTattooPrice())
                .width(req.getTattooWidth())
                .height(req.getTattooHeight())
                .tattooRefference(req.getTattooRefference())
                .appDate(req.getAppointmentDate())
                .appTime(req.getAppointmentTime())
                .phoneNumber(req.getPhoneNumber())
                .email(req.getEmail())
                .build();

        mailService.sendMeMail(smr);
        mailService.sendClientMail(smr);
        return "CREATED";
    }

    public PriceQuoteRes getPriceQuote(GetPriceQuoteReq req) throws JsonProcessingException {


        Double width = req.getSizeWidth();
        Double height = req.getSizeHeight();
        double area = width * height;
        //String sizeCode = sizeCodeFromArea(area);

        String prompt = buildPrompt(height, width, area);

        MultipartFile tattooRefference = req.getTattooRefference();
        String base64Image =  toBase64(tattooRefference);

        String endpoint = System.getenv("AZURE_OPENAI_ENDPOINT")
                .replaceAll("/$", "");
        String apiKey = System.getenv("AZURE_OPENAI_KEY");
        String deployment = System.getenv("AZURE_OPENAI_DEPLOYMENT");
        String apiVersion = System.getenv("AZURE_OPENAI_API_VERSION");

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

    private String buildPrompt(double height, double width, double area) {
        return """
                Elemezd a tetoválás fényképét és a vendég által megadott méreteket (cm).
                
                              A feladatod: a tetoválás kategorizálása és az ár kiszámítása a következő árlista alapján.
                              A számítás során kizárólag a megadott árlistát használhatod.
                              Ne interpolálj, ne becsülj, ne módosíts árakat.
                              Méretadatok:
                               - magasság: %s cm
                               - szélesség: %s cm
                               - terület: %s cm²
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
                """.formatted(height, width, area);
    }

}
