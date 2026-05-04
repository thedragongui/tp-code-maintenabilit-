package com.taverne.kiss.service;

import com.taverne.kiss.model.ItemType;
import com.taverne.kiss.model.TabItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class TabCalculationService {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");

    public BigDecimal calculateTotal(List<TabItem> items) {
        BigDecimal subtotal = items.stream()
                .map(TabItem::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!shouldApplyDiscount(items)) {
            return subtotal.setScale(2, RoundingMode.HALF_UP);
        }

        return subtotal
                .multiply(BigDecimal.ONE.subtract(DISCOUNT_RATE))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean shouldApplyDiscount(List<TabItem> items) {
        boolean hasMeal = items.stream().anyMatch(item -> item.type() == ItemType.MEAL);
        boolean hasDrink = items.stream().anyMatch(item -> item.type() == ItemType.DRINK);
        return hasMeal && hasDrink;
    }
}
