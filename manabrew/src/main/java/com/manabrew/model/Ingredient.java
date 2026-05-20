package com.manabrew.model;

// simple value object representing one ingredient in a recipe
public class Ingredient {
    private String name;

    public Ingredient(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}