package com.manabrew.model;

// plain data object - Gson serializes and deserializes this to/from shift_logs.json
public class ShiftLog {
    private String timestamp;
    private int potionsDelivered;
    private int finalGold;

    public ShiftLog(String timestamp, int potionsDelivered, int finalGold) {
        this.timestamp = timestamp;
        this.potionsDelivered = potionsDelivered;
        this.finalGold = finalGold;
    }

    public String getTimestamp() { return timestamp; }
    public int getPotionsDelivered() { return potionsDelivered; }
    public int getFinalGold() { return finalGold; }
}