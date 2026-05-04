package com.taverne.solid.service;

import com.taverne.solid.model.OrderLine;
import com.taverne.solid.repository.INotificationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final INotificationRepository notificationRepository;
    private final List<String> orderHistory = new ArrayList<>();

    public OrderService(INotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void registerOrder(List<OrderLine> lines, double total) {
        String message = "Order accepted with " + lines.size() + " lines, total=" + total + " at " + Instant.now();
        orderHistory.add(message);
        notificationRepository.save(message);
    }

    public List<String> getOrderHistory() {
        return List.copyOf(orderHistory);
    }
}
