package com.manabrew.inventory;
import com.manabrew.model.Ingredient;
import java.util.HashMap;

public class Pantry {
    private HashMap<String, Integer> stock = new HashMap<>();

    public Pantry() {
        // buffed starting stock, we kept running out during testing
        stock.put("water", 500);
        stock.put("dragon scale", 200); 
        stock.put("fairy dust", 200);
        stock.put("fire pepper", 200); 
    }

    // heavily locked down to stop 2 players from duping ingredients at the same time
    public synchronized boolean takeIngredients(String[] stuffWanted) {
        // sanity check loop
        for (String item : stuffWanted) {
            item = item.trim();
            if (stock.getOrDefault(item, 0) <= 0) {
                return false; 
            }
        }
        
        // actual deduction loop
        for (String item : stuffWanted) {
            item = item.trim();
            int currentQty = stock.get(item);
            stock.put(item, currentQty - 1);
            // System.out.println("debug: " + item + " taken. left: " + (currentQty - 1));
        }
        return true;
    }
}