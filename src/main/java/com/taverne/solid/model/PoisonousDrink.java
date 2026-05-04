package com.taverne.solid.model;

public class PoisonousDrink extends ConsumableItem {

    public PoisonousDrink(String name) {
        super(name);
    }

    @Override
    public boolean isSafeToConsume() {
        return false;
    }
}
