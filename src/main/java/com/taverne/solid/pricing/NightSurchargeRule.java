package com.taverne.solid.pricing;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;

@Component
@Order(20)
public class NightSurchargeRule implements PricingRule {

    private static final double NIGHT_SURCHARGE_RATE = 0.10;
    private static final LocalTime NIGHT_START = LocalTime.of(22, 0);

    private final Clock clock;

    public NightSurchargeRule(Clock clock) {
        this.clock = clock;
    }

    @Override
    public double apply(double baseTotal) {
        LocalTime now = LocalTime.now(clock);
        if (now.isBefore(NIGHT_START)) {
            return baseTotal;
        }
        return baseTotal * (1 + NIGHT_SURCHARGE_RATE);
    }
}
