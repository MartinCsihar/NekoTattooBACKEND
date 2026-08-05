package com.testp.tattooappointmentservice.Requests;

import com.testp.tattooappointmentservice.Enums.TattooStyle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TattooAppointmentReq {
    private String email;
    private String phoneNumber;
    private String lastName;
    private String firstName;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private List<Double> width;
    private List<Double> height;
    private TattooStyle style;
    private List<String> bodyParts;
    private List<Integer> tattooPrice;
    private List<MultipartFile> tattooRefferences;
    private Boolean largeTattoo; //When tattoo is bigger than 600cm2
    private Boolean customDesignTattoo;
    private String customDesignTattooText;
}
