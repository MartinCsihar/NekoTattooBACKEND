package com.testp.tattooappointmentservice.Requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SendMailReq {
    private String appId;
    private String firstName;
    private String lastName;
    private List<MultipartFile> tattooRefference;
    private LocalDate appDate;
    private LocalTime appTime;
    private List<Integer> price;
    private List<Double> height;
    private List<Double> width;
    private String phoneNumber;
    private String email;
    private List<Boolean> uniqueTattoo;
    private Boolean customDesignTattoo;
    private String customDesignTattooText;
}
