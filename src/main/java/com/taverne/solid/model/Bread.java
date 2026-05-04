package com.taverne.solid.model;

import com.taverne.solid.interfaces.ICookable;

public class Bread implements ICookable {

    @Override
    public String cook() {
        return "Bread is cooked.";
    }

    @Override
    public String roast() {
        return "Bread is roasted.";
    }
}
