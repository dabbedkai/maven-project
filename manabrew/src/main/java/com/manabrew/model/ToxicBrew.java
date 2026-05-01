package com.manabrew.model;

// inheritance 
public class ToxicBrew extends Potion {
    public ToxicBrew() {
        super("Toxic Brew", 2, 30, new Ingredient[]{ 
            new Ingredient("dragon scale"), 
            new Ingredient("fairy dust") 
        });
    }
}