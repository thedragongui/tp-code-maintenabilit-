package com.taverne.solid.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolidContractsTest {

    @Test
    void lsp_consumableListSupportsPoisonousDrinkWithoutException() {
        List<ConsumableItem> items = List.of(
                new ConsumableItem("Soup"),
                new PoisonousDrink("Toxic Ale")
        );

        long safeCount = items.stream()
                .filter(ConsumableItem::isSafeToConsume)
                .count();

        assertEquals(1, safeCount);
        assertTrue(items.getFirst().isSafeToConsume());
        assertFalse(items.get(1).isSafeToConsume());
    }

    @Test
    void isp_breadAndAleOnlyExposeRelevantActions() {
        Bread bread = new Bread();
        Ale ale = new Ale();

        assertEquals("Bread is cooked.", bread.cook());
        assertEquals("Bread is roasted.", bread.roast());
        assertEquals("Ale is poured into a mug.", ale.pourIntoMug());
    }
}
