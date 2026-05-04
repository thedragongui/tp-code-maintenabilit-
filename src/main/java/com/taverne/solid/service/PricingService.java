package com.taverne.solid.service;

import com.taverne.solid.model.OrderLine;
import com.taverne.solid.pricing.PricingRule;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PricingService {

    private final List<PricingRule> pricingRules;

    public PricingService(List<PricingRule> pricingRules) {
        this.pricingRules = pricingRules;
    }

    public double calculateTotal(List<OrderLine> lines) {
        double total = lines.stream()
                .map(this::lineSubtotal)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();

        for (PricingRule rule : pricingRules) {
            total = rule.apply(total);
        }

        return BigDecimal.valueOf(total)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private BigDecimal lineSubtotal(OrderLine line) {
        return line.unitPrice().multiply(BigDecimal.valueOf(line.quantity()));
    }
}
