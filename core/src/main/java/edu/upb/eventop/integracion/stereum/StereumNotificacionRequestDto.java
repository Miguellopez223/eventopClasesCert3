package edu.upb.eventop.integracion.stereum;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class StereumNotificacionRequestDto {
    @JsonProperty("notification_type")
    private String notificationType;
    private String id;
    private StereumTransactionRequestDto transaction;
    private Long timestamp;
}
