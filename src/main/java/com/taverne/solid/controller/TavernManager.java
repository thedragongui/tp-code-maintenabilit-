package com.taverne.solid.controller;

import com.taverne.solid.model.OrderLine;
import com.taverne.solid.model.OrderRequest;
import com.taverne.solid.model.OrderResponse;
import com.taverne.solid.service.InventoryService;
import com.taverne.solid.service.OrderService;
import com.taverne.solid.service.PricingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/solid")
public class TavernManager {

    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final PricingService pricingService;

    public TavernManager(OrderService orderService, InventoryService inventoryService, PricingService pricingService) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.pricingService = pricingService;
    }

    @PostMapping("/orders")
    public OrderResponse placeOrder(@RequestBody OrderRequest request) {
        List<OrderLine> lines = validateRequest(request);

        inventoryService.verifyAndReserve(lines);
        double total = pricingService.calculateTotal(lines);
        orderService.registerOrder(lines, total);

        return new OrderResponse(total);
    }

    private List<OrderLine> validateRequest(OrderRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "items is required.");
        }

        for (OrderLine line : request.items()) {
            if (line == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "items must not contain null values.");
            }
            if (line.itemName() == null || line.itemName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itemName is required.");
            }
            if (line.unitPrice() == null || line.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unitPrice must be >= 0.");
            }
            if (line.quantity() == null || line.quantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be > 0.");
            }
        }

        return request.items();
    }
}
