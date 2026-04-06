package com.roshambo.models;

// abstraction base class that forces polymorphic movement validation instead of ints
public abstract class GameMove {
    private String moveName;

    public GameMove(String moveName) {
        this.moveName = moveName;
    }

    public String getMoveName() {
        return moveName;
    }

    // compares to returning 1 for victory -1 for defeat and 0 for tie matching constraints
    public abstract int compare(GameMove other);
}