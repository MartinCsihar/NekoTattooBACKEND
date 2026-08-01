package com.testp.tattooappointmentservice.Requests;

import com.testp.tattooappointmentservice.Enums.TattooStyle;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TattooAppointmentReq {
    public String email;
    public String phoneNumber;
    public String lastName;
    public String firstName;
    public LocalDate appointmentDate;
    public LocalTime appointmentTime;
    public Double tattooHeight;
    public Double tattooWidth;
    public TattooStyle style;
    public Integer tattooPrice;
    public MultipartFile tattooRefference;
}
