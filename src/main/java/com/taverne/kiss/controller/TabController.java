package com.taverne.kiss.controller;

import com.taverne.kiss.model.TabCalculationRequest;
import com.taverne.kiss.model.TabCalculationResponse;
import com.taverne.kiss.model.TabItem;
import com.taverne.kiss.service.TabCalculationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/tab")
public class TabController {

    private final TabCalculationService calculationService;

    public TabController(TabCalculationService calculationService) {
        this.calculationService = calculationService;
    }

    @PostMapping("/calculate")
    public TabCalculationResponse calculate(@RequestBody TabCalculationRequest request) {
        List<TabItem> items = requireItems(request);
        validateItems(items);

        BigDecimal total = calculationService.calculateTotal(items);
        return new TabCalculationResponse(total);
    }

    private List<TabItem> requireItems(TabCalculationRequest request) {
        if (request == null || request.items() == null) {
            throw badRequest("items is required.");
        }
        return request.items();
    }

    private void validateItems(List<TabItem> items) {
        for (TabItem item : items) {
            if (item == null) {
                throw badRequest("items must not contain null values.");
            }
            if (item.type() == null) {
                throw badRequest("item.type is required.");
            }
            if (item.price() == null) {
                throw badRequest("item.price is required.");
            }
            if (item.price().compareTo(BigDecimal.ZERO) < 0) {
                throw badRequest("item.price must be >= 0.");
            }
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
