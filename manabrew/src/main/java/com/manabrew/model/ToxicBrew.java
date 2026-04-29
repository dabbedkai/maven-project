package com.manabrew.model;

public class ToxicBrew extends Potion {
    public ToxicBrew() {
        super("toxic brew", 2, 30, new Ingredient[]{ 
            new Ingredient("dragon scale"), 
            new Ingredient("fairy dust") 
        });
    }
}