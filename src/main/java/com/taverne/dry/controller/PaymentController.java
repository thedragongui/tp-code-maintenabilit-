package com.taverne.dry.controller;

import com.taverne.dry.model.PaymentRequest;
import com.taverne.dry.service.PaymentCalculationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentCalculationService paymentCalculationService;

    public PaymentController(PaymentCalculationService paymentCalculationService) {
        this.paymentCalculationService = paymentCalculationService;
    }

    @PostMapping("/warrior")
    public BigDecimal warrior(@RequestBody PaymentRequest request) {
        return paymentCalculationService.calculateWarrior(request);
    }

    @PostMapping("/mage")
    public BigDecimal mage(@RequestBody PaymentRequest request) {
        return paymentCalculationService.calculateMage(request);
    }

    @PostMapping("/rogue")
    public BigDecimal rogue(@RequestBody PaymentRequest request) {
        return paymentCalculationService.calculateRogue(request);
    }
}
