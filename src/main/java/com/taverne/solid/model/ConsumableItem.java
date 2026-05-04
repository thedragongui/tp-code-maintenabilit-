package com.taverne.solid.model;

public class ConsumableItem {

    private final String name;

    public ConsumableItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isSafeToConsume() {
        return true;
    }
}
