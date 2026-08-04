package com.testp.tattooappointmentservice.Responses;

import com.testp.tattooappointmentservice.Enums.TattooCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PriceQuoteForCustomTattooRes {
    private Double estimatedWidth;
    private Double estimatedHeight;
    private Boolean isWrapAround;
    private String complexity;
    private Boolean designNeeded;
    private Boolean isAnimal;
    private Boolean isText;
    private TattooCategory category;
    private Integer price;
    private String reason;
}
