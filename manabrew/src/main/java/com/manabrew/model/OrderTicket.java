package com.manabrew.model;

// represents a single customer order sitting on the board
public class OrderTicket {
    private Potion targetPotion;
    private int timeLeft;
    private String claimedBy;   // null = unclaimed, otherwise holds the player's username

    public OrderTicket(Potion targetPotion, int maxTime) {
        this.targetPotion = targetPotion;
        this.timeLeft = maxTime;
        this.claimedBy = null;
    }

    public Potion getPotion() { return targetPotion; }
    public int getTimeLeft() { return timeLeft; }
    public void tickTimer() { this.timeLeft--; }

    // atomic claim: only the first caller wins; subsequent calls from the same player
    // return true so they don't get locked out of their own order by a race condition
    public synchronized boolean claim(String player) {
        if (claimedBy == null) { claimedBy = player; return true; }
        return claimedBy.equals(player);
    }

    public synchronized String getClaimedBy() { return claimedBy; }
}