package com.roshambo.models;

// concrete scissors rule implementation
public class Scissors extends GameMove {
    public Scissors() {
        super("Scissors");
    }

    @Override
    public int compare(GameMove other) {
        if (other instanceof Scissors) return 0;
        if (other instanceof Paper) return 1;
        return -1; // implies rock logic loss naturally
    }
}