package com.hangman;

public class User {
    
    String username;
    String password;
    int score;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public User(String username, String password, int score) {
        this.username = username;
        this.password = password;
        this.score = score;
    }

    public String toJson() {
            return String.format("{\"username\":\"%s\", \"password\":\"%s\", \"score\":%d}", username, password, score);
        }
}
