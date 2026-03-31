package com.roshambo;

// inheritance from AbstractAccount to represent a user with specific properties like score
public class User extends AbstractAccount {
    private int score;

    public User(String username, String password, int score) {
        super(username, password); 
        this.score = score;
    }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
}

