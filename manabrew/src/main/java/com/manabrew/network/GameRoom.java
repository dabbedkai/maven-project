package com.manabrew.network;

import com.manabrew.inventory.*;
import com.manabrew.model.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameRoom {
    private String roomCode;
    public Pantry roomPantry;
    public StorageBunker<OrderTicket> orders = new StorageBunker<>();
    public CopyOnWriteArrayList<ClientHandler> players = new CopyOnWriteArrayList<>();
    
    private int roundNum = 1;
    public int sharedVaultGold = 100;
    public boolean isShopPhase = true; 
    public String hostPlayer = "";

    public GameRoom(String code) {
        this.roomCode = code;
        this.roomPantry = new Pantry(); 
    }

    public void addPlayer(ClientHandler player, String username) {
        players.add(player);
        if (players.size() == 1) {
            hostPlayer = username; 
        }
        broadcast("\n[ROOM " + roomCode + "] " + username + " joined. Host: " + hostPlayer);
        broadcast("Shop Phase active. Buy items using 'shop <item>'. Host types 'start' to begin round " + roundNum + "!");
    }

    public void broadcast(String msg) {
        for (ClientHandler ch : players) ch.sendMessage(msg);
    }

    public void handleDisconnect(ClientHandler p) {
        players.remove(p);
        if (players.size() > 0 && p.getUsername().equals(hostPlayer)) {
            hostPlayer = players.get(0).getUsername(); 
            broadcast("\n[ALERT] Host disconnected. New host elected: " + hostPlayer);
        }
    }

    public void startRound() {
        if (!isShopPhase) return;
        isShopPhase = false;
        
        broadcast("\n=== ROUND " + roundNum + " HAS STARTED! ===");
        broadcast("Stock is draining, difficulty scaling. Good luck.");
        
        new Thread(() -> {
            Random rng = new Random();
            int roundDurationSeconds = 60 + (roundNum * 10);
            
            while (roundDurationSeconds > 0 && !players.isEmpty()) {
                try {
                    Thread.sleep(1000);
                    roundDurationSeconds--;
                    int spawnRate = Math.max(5, 15 - (roundNum * 2));
                    if (roundDurationSeconds % spawnRate == 0) {
                        String rndType = PotionFactory.ALL_TYPES[rng.nextInt(PotionFactory.ALL_TYPES.length)];
                        Potion newPot = PotionFactory.create(rndType);
                        
                        orders.add(new OrderTicket(newPot, 45));
                        broadcast("\n[ NEW ORDER! ] -> " + newPot.getName());
                    }
                } catch (InterruptedException e) {}
            }
            
            // Round End sequence
            isShopPhase = true;
            roundNum++;
            broadcast("\n=== ROUND END! SHOPPING PHASE ACTIVE ===");
            broadcast("Vault Total: " + sharedVaultGold + "g. Use 'shop <item>'. Host: type 'start'");
            
        }).start();
    }
}