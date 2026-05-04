package com.taverne.solid.pricing;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class KingTaxRule implements PricingRule {

    private static final double KING_TAX_RATE = 0.05;

    @Override
    public double apply(double baseTotal) {
        return baseTotal * (1 + KING_TAX_RATE);
    }
}
