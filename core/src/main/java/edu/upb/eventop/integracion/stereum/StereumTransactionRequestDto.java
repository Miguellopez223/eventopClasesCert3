package edu.upb.eventop.integracion.stereum;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class StereumTransactionRequestDto {
    private String country;
    private BigDecimal amount;

    @JsonProperty("amount_received")
    private BigDecimal amountReceived;

    @JsonProperty("status_description")
    private String statusDescription;

    private String currency;

    private String network;

    @JsonProperty("created_date")
    private Long createdDate;

    @JsonProperty("payment_date")
    private Long paymentDate;

    private String status;
}
