package edu.upb.eventop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service("ganaderMetodoPago")
public class BancoGanaderoImpl implements MetodoPagoService {
    @Override
    public String generQr(BigDecimal monto) {
        return "";
    }
}
