package com.taverne.solid.pricing;

public interface PricingRule {

    double apply(double baseTotal);
}
