package com.testp.tattooappointmentservice.Responses;

import com.testp.tattooappointmentservice.Enums.SizeCode;
import com.testp.tattooappointmentservice.Enums.TattooCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceQuoteRes {
   private String complexity;
   private Boolean isAnimal;
   private Boolean isWrapAround;
   private Boolean isText;
   private TattooCategory category;
   private Integer price;
   private String reason;
   private Boolean designNeeded;
   private Double estimatedWidth;
   private Double estimatedHeight;
   private Boolean notRealMeasure;
}
