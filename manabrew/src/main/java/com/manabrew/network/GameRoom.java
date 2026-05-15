package com.manabrew.network;

import com.manabrew.inventory.*;
import com.manabrew.model.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameRoom {
    private String roomCode;

    // shared state all players in this room read and write
    public Pantry roomPantry;
    public StorageBunker<OrderTicket> orders = new StorageBunker<>();
    public CopyOnWriteArrayList<ClientHandler> players = new CopyOnWriteArrayList<>();

    private int     roundNum      = 1;
    public  int     sharedVaultGold = 100;   // team starts with 100g
    public  boolean isShopPhase   = true;
    public  String  hostPlayer    = "";

    public GameRoom(String code) {
        this.roomCode    = code;
        this.roomPantry  = new Pantry();
    }

    // exposes round number so ClientHandler can display it without direct field access
    public int getRoundNum() { return roundNum; }

    // adds a player; first one in becomes the host
    public void addPlayer(ClientHandler player, String username) {
        players.add(player);
        if (players.size() == 1) hostPlayer = username;
        broadcast("[room " + roomCode + "] " + username + " joined.  host: " + hostPlayer);
        broadcast("shop phase active. buy ingredients with  shop <item>.  host: type  start  when ready.");
    }

    // sends a message to every player in the room
    // each sendMessage call triggers a UI redraw on that player's terminal
    public void broadcast(String msg) {
        for (ClientHandler ch : players) ch.sendMessage(msg);
    }

    // removes a disconnected player; promotes a new host if needed
    public void handleDisconnect(ClientHandler p) {
        players.remove(p);
        if (!players.isEmpty() && p.getUsername().equals(hostPlayer)) {
            hostPlayer = players.get(0).getUsername();
            broadcast("[alert] host left. new host: " + hostPlayer);
        }
    }

    // flips from shop phase to brew phase and starts the order-spawning loop
    public void startRound() {
        if (!isShopPhase) return;
        isShopPhase = false;
        broadcast("=== round " + roundNum + " has started! ===");

        new Thread(() -> {
            Random rng = new Random();
            int timeLeft = 60 + (roundNum * 10);   // rounds get longer as difficulty scales

            while (timeLeft > 0 && !players.isEmpty()) {
                try {
                    Thread.sleep(1000);
                    timeLeft--;

                    // spawn rate shrinks each round so orders pile up faster
                    int spawnEvery = Math.max(5, 15 - (roundNum * 2));
                    if (timeLeft % spawnEvery == 0) {
                        String type  = PotionFactory.ALL_TYPES[rng.nextInt(PotionFactory.ALL_TYPES.length)];
                        Potion newPot = PotionFactory.create(type);
                        orders.add(new OrderTicket(newPot, 45));
                        broadcast("[ new order ] --> " + newPot.getName());
                    }
                } catch (InterruptedException ignored) {}
            }

            // round over: flip back to shop and increment round counter
            isShopPhase = true;
            roundNum++;
            broadcast("=== round end!  shop phase is back ===");
            broadcast("vault: " + sharedVaultGold + "g.   host: type  start  for the next round.");

        }).start();
    }
}