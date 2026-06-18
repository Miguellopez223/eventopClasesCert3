package edu.upb.eventop.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service("stereumMetodoPago")
public class StereumServiceImpl implements MetodoPagoService{
    @Override
    public String generQr(BigDecimal monto) {
        return "";
    }
}
