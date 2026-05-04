package com.taverne.solid.pricing;

import com.taverne.solid.model.OrderLine;
import com.taverne.solid.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingRulesTest {

    @Test
    void pricingService_appliesInjectedRulesWithoutCodeChange() {
        PricingRule plusTenRule = total -> total + 10;
        PricingRule doubleRule = total -> total * 2;

        PricingService pricingService = new PricingService(List.of(plusTenRule, doubleRule));

        double total = pricingService.calculateTotal(List.of(
                new OrderLine("bread", new BigDecimal("5.00"), 2)
        ));

        assertEquals(40.0, total, 0.0001);
    }

    @Test
    void nightSurchargeRule_appliesAfter22h() {
        Clock lateClock = Clock.fixed(Instant.parse("2026-05-04T20:30:00Z"), ZoneId.of("Europe/Paris"));
        NightSurchargeRule rule = new NightSurchargeRule(lateClock);

        assertEquals(110.0, rule.apply(100.0), 0.0001);
    }

    @Test
    void weekendDiscountRule_appliesOnWeekend() {
        Clock sundayClock = Clock.fixed(Instant.parse("2026-05-03T10:00:00Z"), ZoneId.of("Europe/Paris"));
        WeekendDiscountRule rule = new WeekendDiscountRule(sundayClock);

        assertEquals(95.0, rule.apply(100.0), 0.0001);
    }
}
