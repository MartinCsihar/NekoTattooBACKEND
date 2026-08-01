package com.testp.tattooappointmentservice.Requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SendMailReq {
    public String appId;
    public String firstName;
    public String lastName;
    public MultipartFile tattooRefference;
    public LocalDate appDate;
    public LocalTime appTime;
    public Integer price;
    public Double height;
    public Double width;
    public String phoneNumber;
    public String email;
}
