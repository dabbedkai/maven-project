package com.roshambo;

// abstraction class to represent common account properties and behaviors for both User and Admin
public abstract class AbstractAccount {
    protected String username;
    protected String password;

    public AbstractAccount(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
