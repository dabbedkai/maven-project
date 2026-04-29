package com.manabrew.model;

public class HealingElixir extends Potion {
    public HealingElixir() {
        super("healing elixir", 1, 15, new Ingredient[]{ 
            new Ingredient("water") 
        });
    }
}