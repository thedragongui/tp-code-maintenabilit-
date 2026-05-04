package com.taverne.solid.model;

import java.math.BigDecimal;

public record OrderLine(
        String itemName,
        BigDecimal unitPrice,
        Integer quantity
) {
}
