package com.taverne.solid.pricing;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

@Component
@Order(30)
public class WeekendDiscountRule implements PricingRule {

    private static final double WEEKEND_DISCOUNT_RATE = 0.05;
    private static final Set<DayOfWeek> WEEKEND_DAYS = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private final Clock clock;

    public WeekendDiscountRule(Clock clock) {
        this.clock = clock;
    }

    @Override
    public double apply(double baseTotal) {
        DayOfWeek dayOfWeek = LocalDate.now(clock).getDayOfWeek();
        if (!WEEKEND_DAYS.contains(dayOfWeek)) {
            return baseTotal;
        }
        return baseTotal * (1 - WEEKEND_DISCOUNT_RATE);
    }
}
