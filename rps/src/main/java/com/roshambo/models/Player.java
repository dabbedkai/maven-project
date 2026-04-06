package com.roshambo.models;

// inheritance from account to represent a user with specific properties like score
public class Player extends Account {
    private int score; // used for total match victories
    private int matchesPlayed; // used to calculate win percentage
    
    // marks field transient stopping gson crashing attempting to write abstracts randomly into files
    private transient GameMove currentMove;

    public Player(String username, String password, int score, int matchesPlayed) {
        super(username, password); 
        this.score = score;
        this.matchesPlayed = matchesPlayed;
    }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    
    public int getMatchesPlayed() { return matchesPlayed; }
    public void setMatchesPlayed(int matchesPlayed) { this.matchesPlayed = matchesPlayed; }
    
    public GameMove getCurrentMove() { return currentMove; }
    public void setCurrentMove(GameMove currentMove) { this.currentMove = currentMove; }

    // securely increments score internally via data class logic encapsulation directly 
    public void incrementScore() {
        this.score++;
    }

    // calculates the win rate percentage. Returns 0 if no matches are played to prevent errors.
    public double getWinRate() {
        if (matchesPlayed == 0) {
            return 0.0;
        }
        return ((double) score / matchesPlayed) * 100.0;
    }
}