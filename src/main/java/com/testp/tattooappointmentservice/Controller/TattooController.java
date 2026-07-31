package com.testp.tattooappointmentservice.Controller;

import com.testp.tattooappointmentservice.Requests.GetPriceQuoteReq;
import com.testp.tattooappointmentservice.Requests.TattooAppointmentReq;
import com.testp.tattooappointmentservice.Responses.PriceQuoteRes;
import com.testp.tattooappointmentservice.Service.TattooService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.netty.http.Cookies;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TattooController {

    private final TattooService tattooService;

    @PostMapping("/saveAppointment")
    public ResponseEntity<?> saveAppointment(@ModelAttribute TattooAppointmentReq req){
        try{
            return new ResponseEntity<>(tattooService.saveTattooAppointment(req), HttpStatus.CREATED);
        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/getPriceQuote")
    public ResponseEntity<?> getPriceQuote(@ModelAttribute GetPriceQuoteReq req,
                                           HttpServletRequest request,
                                           HttpServletResponse response){
        try {
            Cookie[] cookies  = request.getCookies();
            int count = 0;
            if(cookies != null){
                for(Cookie cookie : cookies){
                    if(cookie.getName().equals("price_quote_count")){
                        count = Integer.parseInt(cookie.getValue());
                    }
                }
            }
            if(count >= 2){
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Napi limit elérve!");
            }
            PriceQuoteRes priceQuoteRes = tattooService.getPriceQuote(req);
            count++;

            Cookie cookie = new Cookie("price_quote_count", String.valueOf(count));
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(60*60*24);
            cookie.setSecure(false);
            response.addCookie(cookie);

            return  ResponseEntity.ok(priceQuoteRes);
        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
