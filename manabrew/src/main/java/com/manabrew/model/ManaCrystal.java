package com.manabrew.model;

public class ManaCrystal extends Potion {
    public ManaCrystal() {
        super("mana crystal", 2, 25, new Ingredient[]{ 
            new Ingredient("water"), 
            new Ingredient("fairy dust") 
        });
    }
}