package com.manabrew.model;

public class Potion implements Brewable {
    private String name;
    private int tier;
    private int price;
    private Ingredient[] recipe;

    public Potion(String name, int tier, int price, Ingredient[] recipe) {
        this.name = name;
        this.tier = tier;
        this.price = price;
        this.recipe = recipe;
    }

    public String getName() { return name; }
    public int getTier() { return tier; }
    public int getPrice() { return price; }
    public Ingredient[] getRecipe() { return recipe; }

    public String getRecipeString() {
        StringBuilder sb = new StringBuilder();
        for (Ingredient i : recipe) {
            sb.append(i.getName()).append(", ");
        }
        // Remove trailing comma
        return sb.substring(0, sb.length() - 2); 
    }

    // Advanced factory pattern holding all your potions!
    public static Potion create(String type) {
        switch (type.toLowerCase()) {
            case "healing": 
                return new Potion("Healing Elixir", 1, 15, new Ingredient[]{ new Ingredient("water") });
            case "toxic": 
                return new Potion("Toxic Brew", 2, 30, new Ingredient[]{ new Ingredient("dragon scale"), new Ingredient("fairy dust") });
            case "fireball": 
                return new Potion("Fireball Potion", 3, 50, new Ingredient[]{ new Ingredient("dragon scale"), new Ingredient("fire pepper") });
            case "mana": 
                return new Potion("Mana Crystal", 2, 25, new Ingredient[]{ new Ingredient("water"), new Ingredient("fairy dust") });
            default: return null;
        }
    }
}