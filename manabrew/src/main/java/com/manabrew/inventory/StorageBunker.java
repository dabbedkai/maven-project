package com.manabrew.inventory;
import java.util.ArrayList;

public class StorageBunker<T> {
    private ArrayList<T> items = new ArrayList<>();

    public synchronized void add(T item) {
        items.add(item);
    }

    public synchronized boolean remove(T item) {
        return items.remove(item);
    }

    // gives a safe snapshot to prevent ConcurrentModificationExceptions while server is running timers
    public synchronized ArrayList<T> getSnapshot() {
        return new ArrayList<>(items); 
    }
}