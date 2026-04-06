package com.roshambo.models;

// concrete rock rule implementation
public class Rock extends GameMove {
    public Rock() {
        super("Rock");
    }

    @Override
    public int compare(GameMove other) {
        if (other instanceof Rock) return 0;
        if (other instanceof Scissors) return 1;
        return -1; // implies paper logic loss naturally
    }
}