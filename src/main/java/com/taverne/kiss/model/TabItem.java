package com.taverne.kiss.model;

import java.math.BigDecimal;

public record TabItem(
        String name,
        ItemType type,
        BigDecimal price
) {
}
