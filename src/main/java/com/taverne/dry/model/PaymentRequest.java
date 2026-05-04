package com.taverne.dry.model;

import java.math.BigDecimal;

public record PaymentRequest(
        BigDecimal price,
        Integer quantity
) {
}
