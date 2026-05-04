package com.taverne.solid.model;

import java.util.List;

public record OrderRequest(
        List<OrderLine> items
) {
}
