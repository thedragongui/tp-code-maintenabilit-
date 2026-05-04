package com.taverne.solid.repository;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
@Primary
public class InMemoryNotificationRepository implements INotificationRepository {

    private final List<String> messages = new ArrayList<>();

    @Override
    public void save(String message) {
        messages.add(message);
        System.out.println("[InMemoryNotificationRepository] " + message);
    }

    public List<String> findAll() {
        return Collections.unmodifiableList(messages);
    }
}
