package com.taverne.solid.repository;

import org.springframework.stereotype.Repository;

@Repository
public class SqlNotificationRepository implements INotificationRepository {

    @Override
    public void save(String message) {
        System.out.println("[SqlNotificationRepository] Persist notification: " + message);
    }
}
