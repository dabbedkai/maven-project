package com.roshambo.models;

public class RoshamboResult {
    private String player1;
    private String player2;
    private int p1RoundsWon;
    private int p2RoundsWon;
    private String matchWinner;

    public RoshamboResult(String player1, String player2, int p1RoundsWon, int p2RoundsWon, String matchWinner) {
        this.player1 = player1;
        this.player2 = player2;
        this.p1RoundsWon = p1RoundsWon;
        this.p2RoundsWon = p2RoundsWon;
        this.matchWinner = matchWinner;
    }

    // formats the match result into a readable string for display or logging purposes
    public String getFormattedSummary() {
        return "MATCH RESULT: [" + player1 + ": " + p1RoundsWon + " pts] VS [" + 
               player2 + ": " + p2RoundsWon + " pts] -> WINNER: " + matchWinner;
    }
}