package com.manabrew.model;

public class OrderTicket {
    private Potion targetPotion;
    private int timeLeft;
    private String claimedBy;

    public OrderTicket(Potion targetPotion, int maxTime) {
        this.targetPotion = targetPotion;
        this.timeLeft = maxTime;
        this.claimedBy = null;
    }

    public Potion getPotion() { return targetPotion; }
    public int getTimeLeft() { return timeLeft; }
    public void tickTimer() { this.timeLeft--; }

    // Atomic locks!
    public synchronized boolean claim(String player) {
        if (claimedBy == null) {
            claimedBy = player;
            return true;
        }
        return claimedBy.equals(player); 
    }
    public synchronized String getClaimedBy() { return claimedBy; }
}