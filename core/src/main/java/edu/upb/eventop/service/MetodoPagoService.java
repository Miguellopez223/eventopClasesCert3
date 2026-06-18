package edu.upb.eventop.service;

import java.math.BigDecimal;

public interface MetodoPagoService {
    String generQr(BigDecimal monto);
}
