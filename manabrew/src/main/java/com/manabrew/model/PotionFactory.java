package com.manabrew.model;

public class PotionFactory {
    public static Potion create(String type) {
        switch (type.toLowerCase()) {
            case "healing": return new HealingElixir();
            case "toxic": return new ToxicBrew();
            case "fireball": return new FireballPotion();
            case "mana": return new ManaCrystal();
            default: return null; 
        }
    }
}