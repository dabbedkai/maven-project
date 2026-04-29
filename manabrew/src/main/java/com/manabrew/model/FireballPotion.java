package com.manabrew.model;

public class FireballPotion extends Potion {
    public FireballPotion() {
        super("fireball potion", 3, 50, new Ingredient[]{ 
            new Ingredient("dragon scale"), 
            new Ingredient("fire pepper") 
        });
    }
}