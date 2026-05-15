package com.manabrew.model;

// factory that builds concrete Potion instances by name
// the anonymous subclass trick satisfies the "can't instantiate abstract" rule while keeping this lean
public class PotionFactory {

    // all valid type strings - GameRoom uses this array to pick random orders
    public static final String[] ALL_TYPES = {
        "healing", "toxic", "fireball", "mana", "shadow",
        "stardust", "volcano", "moon-tear", "eclipse", "phoenix"
    };

    // returns a fully configured Potion for the given type string, or null if unknown
    public static Potion create(String type) {
        switch (type.toLowerCase()) {
            case "healing":   return build("Healing Elixir",  1,  15, "water",       "lunar shard");
            case "toxic":     return build("Toxic Brew",      2,  30, "dragon scale", "fairy dust");   // volatile!
            case "fireball":  return build("Fireball",        3,  40, "dragon scale", "fire pepper");
            case "mana":      return build("Mana Crystal",    1,  10, "water",        "fairy dust");
            case "shadow":    return build("Shadow Draft",    3,  50, "void extract", "dragon scale");
            case "stardust":  return build("Stardust Flask",  2,  25, "fairy dust",   "lunar shard");
            case "volcano":   return build("Volcano Sludge",  3,  60, "fire pepper",  "void extract");
            case "moon-tear": return build("Lunar Tear",      4, 100, "water",        "void extract");
            case "eclipse":   return build("Eclipse",         4,  80, "void extract", "lunar shard");
            case "phoenix":   return build("Phoenix Down",    5, 120, "fire pepper",  "fairy dust");
            default: return null;
        }
    }

    // wires up an anonymous Potion subclass so we don't need a concrete class per potion
    private static Potion build(String name, int tier, int price, String ing1, String ing2) {
        return new Potion(name, tier, price, new Ingredient[]{ new Ingredient(ing1), new Ingredient(ing2) }) {};
    }
}