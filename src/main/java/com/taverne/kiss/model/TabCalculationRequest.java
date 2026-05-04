package com.taverne.kiss.model;

import java.util.List;

public record TabCalculationRequest(
        List<TabItem> items
) {
}
