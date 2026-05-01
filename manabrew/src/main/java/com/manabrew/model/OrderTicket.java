package com.manabrew.model;

public class OrderTicket {
    private Potion targetPotion; // uses the base reference class
    private int timeLeft; 

    public OrderTicket(Potion targetPotion, int maxTime) {
        this.targetPotion = targetPotion;
        this.timeLeft = maxTime;
    }

    public Potion getPotion() { return targetPotion; }
    public int getTimeLeft() { return timeLeft; }
    
    public void tickTimer() { this.timeLeft--; }
}