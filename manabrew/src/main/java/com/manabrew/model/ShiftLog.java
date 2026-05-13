package com.manabrew.model;

// pojo specifically so gson can parse the save file back and forth easily
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