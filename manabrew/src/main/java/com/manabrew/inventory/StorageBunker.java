package com.manabrew.inventory;

import java.util.ArrayList;

// generic thread-safe list used for active orders (and anything else we need to share between threads)
public class StorageBunker<T> {
    private ArrayList<T> items = new ArrayList<>();

    public synchronized void add(T item) {
        items.add(item);
    }

    public synchronized boolean remove(T item) {
        return items.remove(item);
    }

    // returns a snapshot copy so callers can iterate without hitting ConcurrentModificationException
    // if another thread removes something mid-loop
    public synchronized ArrayList<T> getSnapshot() {
        return new ArrayList<>(items);
    }
}