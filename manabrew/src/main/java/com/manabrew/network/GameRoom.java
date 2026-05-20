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
    
    // quota tracking
    public int currentQuota = 0;
    public int goldEarnedThisRound = 0;

    public GameRoom(String code) {
        this.roomCode    = code;
        this.roomPantry  = new Pantry();
    }

    // exposes round number so the UI can draw it
    public int getRoundNum() { return roundNum; }

    // figures out how much gold the team needs to survive this round
    // scales up based on how many players are in the room and what round it is
    public int calculateQuota() {
        return (30 * roundNum) + (20 * players.size() * roundNum);
    }

    // adds a player; first one in becomes the host
    public void addPlayer(ClientHandler player, String username) {
        players.add(player);
        if (players.size() == 1) hostPlayer = username;
        broadcast("[room " + roomCode + "] " + username + " joined.  host: " + hostPlayer);
        broadcast("shop phase active. buy ingredients with  shop <item>.  host: type  start  when ready.");
    }

    // blasts a message to every player in the room, forcing a screen redraw
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

    // flips from shop phase to brew phase, locks in the quota, and starts the timer
    public void startRound() {
        if (!isShopPhase) return;
        isShopPhase = false;
        
        currentQuota = calculateQuota();
        goldEarnedThisRound = 0;
        
        broadcast(TerminalColors.YELLOW + "=== round " + roundNum + " started! quota: " + currentQuota + "g ===" + TerminalColors.RESET);

        new Thread(() -> {
            Random rng = new Random();
            int timeLeft = 60 + (roundNum * 10); // slightly longer timer each round

            while (timeLeft > 0 && !players.isEmpty()) {
                try {
                    Thread.sleep(1000);
                    timeLeft--;

                    // orders pile up way faster in later rounds
                    int spawnEvery = Math.max(4, 15 - (roundNum * 2));
                    if (timeLeft % spawnEvery == 0) {
                        String type  = PotionFactory.ALL_TYPES[rng.nextInt(PotionFactory.ALL_TYPES.length)];
                        Potion newPot = PotionFactory.create(type);
                        orders.add(new OrderTicket(newPot, 45));
                        broadcast("[ new order ] --> " + newPot.getName());
                    }
                } catch (InterruptedException ignored) {}
            }

            // round timer ran out - check if they hit the quota
            if (goldEarnedThisRound < currentQuota) {
                isShopPhase = true;
                broadcast(TerminalColors.RED + "=== GAME OVER! ===" + TerminalColors.RESET);
                broadcast("you only made " + goldEarnedThisRound + "g out of the " + currentQuota + "g required.");
                broadcast("the tavern went bankrupt. wiping everything and resetting back to round 1...");
                
                // reset everything back to square one
                roundNum = 1;
                sharedVaultGold = 100;
                roomPantry = new Pantry(); 
                orders = new StorageBunker<>();
            } else {
                // survived the round
                isShopPhase = true;
                roundNum++;
                broadcast(TerminalColors.GREEN + "=== quota met! (" + goldEarnedThisRound + "/" + currentQuota + "g) ===" + TerminalColors.RESET);
                broadcast("vault: " + sharedVaultGold + "g. shop phase is back. host types  start  when ready.");
            }

        }).start();
    }
}