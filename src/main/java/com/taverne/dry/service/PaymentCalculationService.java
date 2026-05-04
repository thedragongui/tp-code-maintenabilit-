package com.taverne.dry.service;

import com.taverne.dry.model.PaymentRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class PaymentCalculationService {

    private static final BigDecimal KING_TAX_RATE = new BigDecimal("0.05");
    private static final BigDecimal WARRIOR_SURCHARGE = new BigDecimal("2.00");

    public BigDecimal calculateWarrior(PaymentRequest request) {
        return calculateWithOptions(request, true, WARRIOR_SURCHARGE);
    }

    public BigDecimal calculateMage(PaymentRequest request) {
        return calculateWithOptions(request, false, BigDecimal.ZERO);
    }

    public BigDecimal calculateRogue(PaymentRequest request) {
        return calculateWithOptions(request, true, BigDecimal.ZERO);
    }

    private BigDecimal calculateWithOptions(PaymentRequest request, boolean applyTax, BigDecimal surcharge) {
        validate(request);

        BigDecimal subtotal = calculateSubtotal(request);
        BigDecimal tax = applyTax ? calculateTax(subtotal) : BigDecimal.ZERO;
        return subtotal.add(tax).add(surcharge).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSubtotal(PaymentRequest request) {
        return request.price().multiply(BigDecimal.valueOf(request.quantity()));
    }

    private BigDecimal calculateTax(BigDecimal subtotal) {
        return subtotal.multiply(KING_TAX_RATE);
    }

    private void validate(PaymentRequest request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required.");
        }
        if (request.price() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "price is required.");
        }
        if (request.quantity() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "quantity is required.");
        }
        if (request.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "price must be >= 0.");
        }
        if (request.quantity() < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "quantity must be >= 0.");
        }
    }
}
