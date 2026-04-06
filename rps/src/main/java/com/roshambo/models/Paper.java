package com.roshambo.models;

// concrete paper rule implementation
public class Paper extends GameMove {
    public Paper() {
        super("Paper");
    }

    @Override
    public int compare(GameMove other) {
        if (other instanceof Paper) return 0;
        if (other instanceof Rock) return 1;
        return -1; // implies scissors logic loss naturally
    }
}