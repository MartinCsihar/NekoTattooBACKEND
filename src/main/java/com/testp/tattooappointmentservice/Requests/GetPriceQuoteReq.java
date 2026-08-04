package com.testp.tattooappointmentservice.Requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetPriceQuoteReq {
    private Double sizeWidth;
    private Double sizeHeight;
    private MultipartFile tattooRefference;
}
