package com.manabrew.inventory;
import java.util.ArrayList;

// the generic class requirement for the rubric
public class StorageBunker<T> {
    private ArrayList<T> items = new ArrayList<>();

    public synchronized void add(T item) {
        items.add(item);
    }

    public synchronized boolean remove(T item) {
        return items.remove(item);
    }

    // this snapshot method is needed because iterating over a list while a client
    // deletes something out of it crashes the whole game (ConcurrentModificationException lol)
    public synchronized ArrayList<T> getSnapshot() {
        return new ArrayList<>(items); 
    }
}