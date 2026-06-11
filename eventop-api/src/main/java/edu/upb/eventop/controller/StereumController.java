package edu.upb.eventop.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.upb.eventop.integracion.stereum.StereumNotificacionRequestDto;
import lombok.AllArgsConstructor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stereum")
public class StereumController {

    @Value("${stereum.secret.key:DASDASDADASDAS}")
    private String stereumApiKey;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Void> outbound(
            @RequestHeader("X-Signature") String signature,
            @RequestHeader("X-Timestamp") int xTimestamp,
            @RequestBody String body) throws Exception {

        String hmac = new HmacUtils(HmacAlgorithms.HMAC_SHA_256,
                stereumApiKey.getBytes(StandardCharsets.UTF_8))
                .hmacHex(body.getBytes(StandardCharsets.UTF_8));
        if (!signature.equals(hmac)) {
            throw new Exception("MessageCode.SIGN_REQUEST_INVALID");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        StereumNotificacionRequestDto notificacion = objectMapper.readValue(body, StereumNotificacionRequestDto.class);
        try {
            log.info("Stereum notificacion received: {}", notificacion.getTransaction().getStatusDescription());
            return ok().build();
        } catch (Exception e) {
            throw e ;
        }
    }
}
