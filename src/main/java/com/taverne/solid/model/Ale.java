package com.taverne.solid.model;

import com.taverne.solid.interfaces.IPourable;

public class Ale implements IPourable {

    @Override
    public String pourIntoMug() {
        return "Ale is poured into a mug.";
    }
}
