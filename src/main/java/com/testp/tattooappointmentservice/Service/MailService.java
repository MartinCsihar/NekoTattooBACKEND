package com.testp.tattooappointmentservice.Service;

import com.testp.tattooappointmentservice.Requests.SendMailReq;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class MailService {
    @Value("${MAIL_SENDER_EMAIL}")
    private String mailSenderEmail;

    private final JavaMailSender sender;

    public void sendMeMail(SendMailReq req) throws MessagingException, IOException {
        MimeMessage mimeMessage = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        String htmlContent = generateMail(req, false, req.getCustomDesignTattoo());

        helper.setSubject("Új foglalás! - NekoTattoo");
        helper.setTo(mailSenderEmail);
        helper.setText(htmlContent, true);

        addAttachments(req, helper);

        sender.send(mimeMessage);
    }

    public void sendClientMail(SendMailReq req) throws MessagingException, IOException {
        MimeMessage mimeMessage = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.of("hu", "HU"));
        nf.setMaximumFractionDigits(0);

        String htmlContent = generateMail(req, true, req.getCustomDesignTattoo());

        addAttachments(req, helper);

        helper.setSubject("Új foglalás! - NekoTattoo");
        helper.setTo(req.getEmail());
        helper.setText(htmlContent, true);
        sender.send(mimeMessage);
    }

    private String generateMail(SendMailReq req, boolean forUser, Boolean customDesignTattoo) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.of("hu", "HU"));
        nf.setMaximumFractionDigits(0);
        String formattedPriceSUM = null;
        Integer priceSum = null;
        if(req.getPrice() != null) {
            priceSum = getPriceSum(req);
            formattedPriceSUM = nf.format(priceSum);
        }
        String text;

        if(!forUser){
            if(customDesignTattoo != null && customDesignTattoo){
                text = "Az ügyfél egyik tetoválása egyedi tervezést igényel, melynek elgondolását lentebb találod!";
            }else{
                if(req.getLargeTattoo()!=null && req.getLargeTattoo()){
                    text = "Az ügyfél egyik tetoválása  <strong>600 cm²-nél nagyobb</strong>, így <strong>konzultációra lesz szükséged</strong>, vedd fel vele a kapcsolatot, ha még nem tette volna meg a vendég!";
                }
                else{
                    text = "";
                }
            }

            String finalPriceText = getFinalPriceText(req, nf, formattedPriceSUM);

            StringBuilder bParts = getBodyPartsText(req);

            List<Double> heights = req.getHeight();
            List<Double> widths = req.getWidth();
            StringBuilder sizes = new StringBuilder();
            for (int i = 0; i<heights.size(); i++) {
                Double currHeight = heights.get(i);
                Double currWidth = widths.get(i);
                // 12.5x14 cmF
                String size = "%.1f cm x %.1f cm ".formatted(currHeight, currWidth);
                if (i != heights.size() - 1) {
                    sizes.append(size).append(" | ");
                } else {
                    sizes.append(size);
                }
            }
            String customDesignMessage  = customDesignTattoo != null && customDesignTattoo ? """
                    <div style="background-color: whitesmoke; width: 90%%;margin:auto ;height: 150px; overflow: hidden; padding: 5px; border-radius:10px; word-wrap:break-word;overflow-wrap: break-word; margin-top:20px">
                        <p style="margin:10px; display:block; "><i>%s</i></p>
                    </div>
                    """.formatted(req.getCustomDesignTattooText()) : "";
            return
            """
                    <!DOCTYPE html>
                    <html>
                      <head>
                        <meta charset="UTF-8"><link rel="preconnect" href="https://fonts.googleapis.com">
                        <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                        <link href="https://fonts.googleapis.com/css2?family=Alfa+Slab+One&family=Cinzel+Decorative:wght@400;700;900&family=Playwrite+AT:ital,wght@0,100..400;1,100..400&family=Roboto+Mono:ital,wght@0,100..700;1,100..700&family=Roboto:ital,wght@0,100..900;1,100..900&family=Rubik+Mono+One&display=swap" rel="stylesheet">
                      </head>
                      <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; background-color: #f9f9f9; padding: 20px;">
                        <div style="max-width: 600px; margin: 0 auto; background: #EFE9DF; border-radius: 10px; overflow: hidden; border: 1px solid #eee; box-shadow: 0 4px 10px rgba(0,0,0,0.05);">
                          <div style="background-color: #680E14; color: #ffffff; padding: 30px; text-align: center;">
                            <h1 style="margin: 0; font-size: 24px; letter-spacing: 2px; text-transform: uppercase; font-family:'Cinzel Decorative'">Neko Tattoo</h1>
                            <p style="margin: 10px 0 0 0; opacity: 0.8;">Új foglalás</p>
                          </div>
                          <div style="padding: 30px">
                            <p>Kedves <strong>Kira</strong>!</p>
                            <p><strong>%s %s</strong> imént adott le egy új foglalást! %s</p>
                            <h3 style="border-bottom: 2px solid #f1c40f; padding-bottom: 10px; color: #2c3e50;">Foglalási adatok</h3>
                                    <table style="width:100%%; border-collapse: collapse; margin-top: 20px; justify-self: center;">
                                            <tr style="background-color:rgba(104,14,20, 0.05);">
                                              <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold; width: 40%%;">Foglalás azonosító</td>
                                              <td style="padding: 12px; border: 1px solid #680E14;">%s</td>
                                            </tr>
                                            <tr style="background-color: #EFE9DF;">
                                              <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold;">Időpont:</td>
                                              <td style="padding: 12px; border: 1px solid #680E14;">%s, %s</td>
                                            </tr>
                                            <tr style="background-color: rgba(104,14,20, 0.05);">
                                              <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold;">Testrészek:</td>
                                              <td style="padding: 12px; border: 1px solid #680E14;">%s</td>
                                            </tr>
                                            <tr style="background-color: #EFE9DF;">
                                              <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold; color: #680E14;">Magasság x szélesség:</td>
                                              <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold; color: #680E14;">%s</td>
                                            </tr>
                                            <tr style="background-color: rgba(104,14,20, 0.05);">
                                              <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold; color: #680E14;">Fizetendő:</td>
                                              <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold; color: #680E14;">%s</td>
                                            </tr>
                                     </table>
                                     <table style="width:100%%
                                     ; border-collapse: collapse; margin-top: 20px; justify-self: center;">
                                        <tr style="background-color: rgba(104,14,20, 0.05);">
                                          <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold; width: 40%%;">Teljes név:</td>
                                          <td style="padding: 12px; border: 1px solid #680E14;">%s %s</td>
                                        </tr>
                                        <tr>
                                          <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold;">Telefonszám:</td>
                                          <td style="padding: 12px; border: 1px solid #680E14;">%s</td>
                                        </tr>
                                        <tr style="background-color:rgba(104,14,20, 0.05);">
                                          <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold;">Email:</td>
                                          <td style="padding: 12px; border: 1px solid #680E14; word-break: break-word; overflow-wrap: break-word;">%s</td>
                                        </tr>
                                        <tr>
                                          <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold;">Foglalás:</td>
                                          <td style="padding: 12px; border: 1px solid #680E14;">%s</td>
                                        </tr>
                                      </table>
                                <div>
                                  </div>
                            </div>
                            %s
                            <div style="background-color: #680e14; padding: 20px; text-align: center; font-size: 12px; color: white;margin-top:20px;">
                              <p>© 2026 Neko Tattoo - Kira</p>
                               <p style="font-size:smaller">nekotatoo26@gmail.com</p>
                               <p style="font-size:smaller">+36 30 368 3414</p>
                            </div>
                          </div>
                     </body>
                     </html>
                    """.formatted(
                    req.getLastName(), req.getFirstName(),
                    text,
                    "#"+req.getAppId(),
                    req.getAppDate(), req.getAppTime(),
                    bParts,
                    sizes,
                    finalPriceText,
                    req.getLastName(), req.getFirstName(),
                    req.getPhoneNumber(),
                    req.getEmail(),
                    LocalDate.now(),
                    customDesignMessage

            );
        }
        else{
            StringBuilder bParts = getBodyPartsText(req) ;
        String customDesignMessage = customDesignTattoo != null && customDesignTattoo ? """
                <div style="background-color: whitesmoke; width: 90%%; justify-self:center;margin:auto; height: 150px; overflow: hidden; padding: 5px; border-radius:10px; word-wrap:break-word;overflow-wrap: break-word; ">
                    <p style="margin:10px; display:block; "><i>%s</i></p>
                </div>
                """.formatted(req.getCustomDesignTattooText()) : "";

        if (req.getLargeTattoo()!= null && req.getLargeTattoo()) {
            text = "Az egyik tetoválásod mérete alapján <strong>konzultációra van szükséged</strong>, vedd fel velem a kapcsolatot mihamarabb!";

        } else {
            text = "Ha bármi kérdésed van, nyugodtan vedd fel velem a kapcsolatot!";
        }

            String finalPriceText = getFinalPriceText(req, nf, formattedPriceSUM);

            return """
                        <!DOCTYPE html>
                        <html>
                        <head>
                        <meta charset="UTF-8"><link rel="preconnect" href="https://fonts.googleapis.com">
                        <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                        <link href="https://fonts.googleapis.com/css2?family=Alfa+Slab+One&family=Cinzel+Decorative:wght@400;700;900&family=Playwrite+AT:ital,wght@0,100..400;1,100..400&family=Roboto+Mono:ital,wght@0,100..700;1,100..700&family=Roboto:ital,wght@0,100..900;1,100..900&family=Rubik+Mono+One&display=swap" rel="stylesheet">
                        </head>
                        <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; background-color: #f9f9f9; padding: 20px;">
                        <div style="max-width: 600px; margin: 0 auto; background: #EFE9DF; border-radius: 10px; overflow: hidden; border: 1px solid #eee; box-shadow: 0 4px 10px rgba(0,0,0,0.05);">
                        <div style="background-color: #680E14; color: #ffffff; padding: 30px; text-align: center;">
                        <h1 style="margin: 0; font-size: 24px; letter-spacing: 2px; text-transform: uppercase; font-family:'Cinzel Decorative'">Neko Tattoo</h1>
                        <p style="margin: 10px 0 0 0; opacity: 0.8;">Foglalásod rögzítve</p>
                        </div>
                        <div style="padding: 30px">
                        <p>Kedves <strong>%s</strong>!</p>
                        <p><strong>Rögzítettem</strong> a foglalásod! %s </p>
                        <h3 style="border-bottom: 2px solid #f1c40f; padding-bottom: 10px; color: #2c3e50;">Foglalási adatok</h3>
                        <table style="width:100%%; border-collapse: collapse; margin-top: 20px; justify-self: center;">
                        <tr style="background-color:rgba(104,14,20, 0.05);">
                        <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold; width: 40%%;">Foglalás azonosító</td>
                        <td style="padding: 12px; border: 1px solid #680E14;">%s</td>
                        </tr>
                        <tr style="background-color: #EFE9DF;">
                        <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold;">Időpont:</td>
                        <td style="padding: 12px; border: 1px solid #680E14;">%s, %s</td>
                        </tr>
                        <tr style="background-color: rgba(104,14,20, 0.05);">
                        <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold;">Testrész:</td>
                        <td style="padding: 12px; border: 1px solid #680E14;">%s</td>
                        </tr>
                        <tr style="background-color: #EFE9DF;">
                        <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold; color: #680E14;">Fizetendő:</td>
                        <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold; color: #680E14;">%s</td>
                        </tr>
                        </table>
                        <table style="width:100%%
                        ; border-collapse: collapse; margin-top: 20px; justify-self: center;">
                        <tr style="background-color: rgba(104,14,20, 0.05);">
                        <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold; width: 40%%;">Teljes név:</td>
                        <td style="padding: 12px; border: 1px solid #680E14;">%s %s</td>
                        </tr>
                        <tr>
                        <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold;">Telefonszám:</td>
                        <td style="padding: 12px; border: 1px solid #680E14;">%s</td>
                        </tr>
                        <tr style="background-color:rgba(104,14,20, 0.05);">
                        <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold;">Email:</td>
                        <td style="padding: 12px; border: 1px solid #680E14; word-break: break-word; overflow-wrap: break-word;">%s</td>
                        </tr>
                        <tr>
                        <td style="padding: 12px; border: 1px solid #680E14; font-weight: bold;">Foglalás:</td>
                        <td style="padding: 12px; border: 1px solid #680E14;">%s</td>
                        </tr>
                        </table>

                        <div>
                        </div>
                        </div>
                        %s
                        <div style="background-color: #680e14; padding: 20px; text-align: center; font-size: 12px; color: white;margin-top:20px;">
                            <p>© 2026 Neko Tattoo - Kira</p>
                            <p style="font-size:smaller">nekotatoo26@gmail.com</p>
                            <p style="font-size:smaller">+36 30 368 3414</p>
                        </div>
                        </div>
                        </body>
                        </html>

                        """.formatted(req.getFirstName(),
                        text,
                        "#"+req.getAppId(),
                        req.getAppDate(), req.getAppTime(),
                        bParts,
                        finalPriceText,
                        req.getLastName(), req.getFirstName(),
                        req.getPhoneNumber(),
                        req.getEmail(),
                        LocalDate.now(),
                        customDesignMessage
                );
            }
        }

    private static @NonNull StringBuilder getBodyPartsText(SendMailReq req) {
        List<String> bodyParts = req.getBodyParts();
        StringBuilder bParts =  new StringBuilder();
        for (int i = 0; i < bodyParts.size(); i++) {
            if(i != bodyParts.size() - 1){
                bParts.append(bodyParts.get(i)).append(", ");
            }else{
                bParts.append(bodyParts.get(i));
            }
        }
        return bParts;
    }

    private String getFinalPriceText(SendMailReq req, NumberFormat nf, String formattedPriceSUM) {
        List<String> formattedPrices = new ArrayList<>();
        if(req.getPrice() == null){
            formattedPrices.add("Konzultáció szükséges!");
        }else{
            List<Integer> prices = req.getPrice();
            for (var  price : prices){
                if(price != null){
                    String fPrice = nf.format(price);
                    formattedPrices.add(fPrice);
                }else{
                    formattedPrices.add("Konzultáció szükséges");
                }
            }
        }

        StringBuilder finalPriceText = new StringBuilder();
        boolean largeTattoo = req.getLargeTattoo()!=null && req.getLargeTattoo();

        if(!req.getTattooRefference().isEmpty() && !largeTattoo){
            finalPriceText.append(formattedPriceSUM);
        }else if(largeTattoo){
            for(int i = 0; i < formattedPrices.size(); i++){
                if(i != formattedPrices.size()-1){
                    finalPriceText.append(formattedPrices.get(i)).append(" + ");
                }
                else{
                    finalPriceText.append(formattedPrices.get(i));
                }
            }
        }
        return finalPriceText.toString();
    }

    private static @NonNull Integer getPriceSum(SendMailReq req) {
        int priceSum = 0;
        for (Integer price : req.getPrice()){
            if(price != null){
                priceSum += price;
            }
        }
        return priceSum;
    }

    private void addAttachments(SendMailReq req, MimeMessageHelper helper) throws MessagingException, IOException {
        if (!req.getTattooRefference().isEmpty()) {
            for (int i = 0; i < req.getTattooRefference().size(); i++) {
                helper.addAttachment(
                        "TetoválásReferencia_" + i + "." + Objects.requireNonNull(req.getTattooRefference().get(i).getContentType()).substring(6).strip(),
                        new ByteArrayDataSource(
                                req.getTattooRefference().get(i).getBytes(),
                                req.getTattooRefference().get(i).getContentType()
                        ));
            }
        }
    }
}