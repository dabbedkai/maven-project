package com.manabrew.model;

public class OrderTicket {
    private Potion targetPotion;
    private int timeLeft; // active timer

    public OrderTicket(Potion targetPotion, int maxTime) {
        this.targetPotion = targetPotion;
        this.timeLeft = maxTime;
    }

    public Potion getPotion() { return targetPotion; }
    public int getTimeLeft() { return timeLeft; }
    
    // server runs this every second
    public void tickTimer() { this.timeLeft--; }
}