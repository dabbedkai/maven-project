package com.manabrew.model;

// abstract so we can't instantiate a raw Potion; every real potion is an anonymous subclass from PotionFactory
// implements Brewable to satisfy the interface requirement and inherit calculateBrewTime
public abstract class Potion implements Brewable {
    // protected so subclasses can read them, but outside code must use the getters
    protected String name;
    protected int tier;
    protected int price;
    protected Ingredient[] recipe;

    public Potion(String name, int tier, int price, Ingredient[] recipe) {
        this.name   = name;
        this.tier   = tier;
        this.price  = price;
        this.recipe = recipe;
    }

    public String getName() { return name; }
    public int getTier() { return tier; }
    public int getPrice() { return price; }
    public Ingredient[] getRecipe() { return recipe; }

    // builds a comma-separated readable string of ingredient names for the UI
    public String getRecipeString() {
        StringBuilder sb = new StringBuilder();
        for (Ingredient i : recipe) sb.append(i.getName()).append(", ");
        return sb.substring(0, sb.length() - 2);
    }
}