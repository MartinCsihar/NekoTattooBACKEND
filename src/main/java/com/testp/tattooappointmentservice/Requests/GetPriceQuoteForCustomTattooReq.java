package com.testp.tattooappointmentservice.Requests;

import com.testp.tattooappointmentservice.Enums.TattooStyle;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@Builder
public class GetPriceQuoteForCustomTattooReq {
    private List<MultipartFile> tattooRefference;
    private String customTattooText;
    private double width;
    private double height;
    private TattooStyle tattooStyle;
}
