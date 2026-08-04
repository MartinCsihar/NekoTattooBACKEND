package com.testp.tattooappointmentservice.Models;

import com.testp.tattooappointmentservice.Enums.TattooStyle;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TattooAppointment {
    @Id
    private String appId;
    private String email;
    private String phoneNumber;
    private String lastName;
    private String firstName;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
//    private Double tattooHeight;
//    private Double tattooWidth;

    @Enumerated(EnumType.STRING)
    private TattooStyle style;
    private List<Integer> tattooPrice;

    @PrePersist
    public void prePersist() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        for(int i = 0; i < 4; i++){
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        appId = sb.toString();
    }
}
