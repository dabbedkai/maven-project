package com.manabrew.model;

public interface Brewable {
    default int calculateBrewTime(int tier) {
        return tier * 3; 
    }

    static boolean isVolatile(String a, String b) {
        return (a.equals("dragon scale") && b.equals("fairy dust")) ||
               (a.equals("fairy dust") && b.equals("dragon scale"));
    }
}