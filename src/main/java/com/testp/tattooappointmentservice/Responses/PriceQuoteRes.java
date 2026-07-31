package com.testp.tattooappointmentservice.Responses;

import com.testp.tattooappointmentservice.Enums.SizeCode;
import com.testp.tattooappointmentservice.Enums.TattooCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceQuoteRes {
   public String complexity;
   public Boolean isAnimal;
   public Boolean isWrapAround;
   public Boolean isText;
   public TattooCategory category;
   public Integer price;
   public String reason;
   public Boolean designNeeded;
}
