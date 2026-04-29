package com.manabrew.inventory;
import com.manabrew.model.Ingredient;
import java.util.HashMap;

public class Pantry {
    private HashMap<String, Integer> stock = new HashMap<>();

    public Pantry() {
        stock.put("water", 100);
        stock.put("dragon scale", 50); 
        stock.put("fairy dust", 50);
        stock.put("fire pepper", 50); 
    }

    public synchronized boolean takeIngredients(String[] reqs) {
        for (String item : reqs) {
            item = item.trim();
            if (stock.getOrDefault(item, 0) <= 0) {
                return false;
            }
        }
        for (String item : reqs) {
            item = item.trim();
            stock.put(item, stock.get(item) - 1);
        }
        return true;
    }
}