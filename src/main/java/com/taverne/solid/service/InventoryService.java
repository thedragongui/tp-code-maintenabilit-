package com.taverne.solid.service;

import com.taverne.solid.model.OrderLine;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class InventoryService {

    private final Map<String, Integer> stockByItem = new HashMap<>();

    public InventoryService() {
        stockByItem.put("bread", 50);
        stockByItem.put("ale", 80);
        stockByItem.put("stew", 30);
    }

    public synchronized void verifyAndReserve(List<OrderLine> lines) {
        Map<String, Integer> requestedByItem = aggregateByItem(lines);

        for (Map.Entry<String, Integer> entry : requestedByItem.entrySet()) {
            String itemName = entry.getKey();
            int requestedQuantity = entry.getValue();
            int currentStock = stockByItem.getOrDefault(itemName, 0);

            if (currentStock < requestedQuantity) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Insufficient stock for item '" + itemName + "'."
                );
            }
        }

        for (Map.Entry<String, Integer> entry : requestedByItem.entrySet()) {
            String itemName = entry.getKey();
            int requestedQuantity = entry.getValue();
            stockByItem.compute(itemName, (key, currentStock) -> currentStock - requestedQuantity);
        }
    }

    private Map<String, Integer> aggregateByItem(List<OrderLine> lines) {
        Map<String, Integer> requestedByItem = new HashMap<>();

        for (OrderLine line : lines) {
            String key = normalizeKey(line.itemName());
            requestedByItem.merge(key, line.quantity(), Integer::sum);
        }

        return requestedByItem;
    }

    private String normalizeKey(String rawName) {
        return rawName.trim().toLowerCase(Locale.ROOT);
    }
}
