package com.manabrew.model;

public interface Brewable {
    // base brew time formula; subclasses can override if they want different pacing
    default int calculateBrewTime(int tier) {
        return tier * 3;
    }

    // returns true if two ingredients are a known volatile (explosive) pair
    // currently only dragon scale + fairy dust triggers this
    static boolean isVolatile(String a, String b) {
        return (a.equals("dragon scale") && b.equals("fairy dust")) ||
               (a.equals("fairy dust")   && b.equals("dragon scale"));
    }
}