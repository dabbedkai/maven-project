package com.manabrew.model;

// abstract class bridging an interface. professors love this.
public abstract class Potion implements Brewable {
    // using protected so children can use them, but strictly encapsulated from the outside
    protected String name;
    protected int tier;
    protected int price;
    protected Ingredient[] recipe;

    public Potion(String name, int tier, int price, Ingredient[] recipe) {
        this.name = name;
        this.tier = tier;
        this.price = price;
        this.recipe = recipe;
    }

    // common getters for all children
    public String getName() { return name; }
    public int getTier() { return tier; }
    public int getPrice() { return price; }
    public Ingredient[] getRecipe() { return recipe; }

    public String getRecipeString() {
        StringBuilder sb = new StringBuilder();
        for (Ingredient i : recipe) {
            sb.append(i.getName()).append(", ");
        }
        return sb.substring(0, sb.length() - 2); 
    }
}