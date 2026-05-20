package com.manabrew.inventory;

import java.util.HashMap;
import java.util.Map;

public class Pantry {
    private HashMap<String, Integer> stock = new HashMap<>();

    // price lookup for the shop - static so ClientHandler can call it without a
    // pantry ref
    private static final Map<String, Integer> PRICES = new HashMap<>();
    static {
        PRICES.put("water", 5);
        PRICES.put("dragon scale", 15);
        PRICES.put("fairy dust", 12);
        PRICES.put("fire pepper", 10);
        PRICES.put("lunar shard", 20);
        PRICES.put("void extract", 25);
    }

    public Pantry() {
        // reduced starting stock so the shop phase actually matters
        stock.put("water", 10);
        stock.put("dragon scale", 5);
        stock.put("fairy dust", 5);
        stock.put("fire pepper", 5);
        // rarer ingredients start at zero - players must buy them
        stock.put("lunar shard", 0);
        stock.put("void extract", 0);
    }

    // returns the gold cost for an ingredient, or -1 if it's not sold here
    public static int getPrice(String item) {
        return PRICES.getOrDefault(item.trim().toLowerCase(), -1);
    }

    // lets the UI show how much of each ingredient is currently on the shelf
    public synchronized int getStock(String item) {
        return stock.getOrDefault(item.trim().toLowerCase(), 0);
    }

    // adds qty of an item; creates the entry if it never existed
    public synchronized void addStock(String item, int qty) {
        item = item.trim().toLowerCase();
        stock.put(item, stock.getOrDefault(item, 0) + qty);
    }

    // deducts all requested items atomically - checks first, then deducts
    // synchronized so two players can't pull the last unit at the same time
    public synchronized boolean takeIngredients(String[] items) {
        // verify everything is available
        for (String item : items) {
            if (stock.getOrDefault(item.trim(), 0) <= 0)
                return false;
        }
        // second pass: actually deduct
        for (String item : items) {
            item = item.trim();
            stock.put(item, stock.get(item) - 1);
        }
        return true;
    }
}