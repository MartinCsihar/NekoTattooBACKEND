package com.testp.tattooappointmentservice.Models;

import com.testp.tattooappointmentservice.Enums.TattooStyle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TattooData {
    private Double width;
    private Double height;
    private TattooStyle style;
    private String bodyPart;
    private Integer tattooPrice;
    private List<MultipartFile> tattooRefferences;
    private Boolean largeTattoo; //When tattoo is bigger than 600cm2
    private Boolean customDesignTattoo;
    private String customDesignTattooText;
}
