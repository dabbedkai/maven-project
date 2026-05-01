package com.manabrew.network;

import com.manabrew.inventory.*;
import com.manabrew.model.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    // using the pantry and generics
    private static Pantry tavernPantry = new Pantry();
    public static StorageBunker<OrderTicket> activeOrders = new StorageBunker<>();
    public static int totalGold = 0;
    
    // list needs to be copy-on-write so we dont get sync crashes during broadcasts
    private static CopyOnWriteArrayList<ClientHandler> activePlayers = new CopyOnWriteArrayList<>();
    
    // just throwing recipes here to pick from randomly
    private static final String[] ALL_RECIPES = {"healing", "toxic", "fireball", "mana"};
    private static Random randGen = new Random();

    public static synchronized void addGold(int amount) {
        totalGold += amount;
    }

    // call this after they type their name to avoid terminal text gore
    public static void addPlayer(ClientHandler player) {
        activePlayers.add(player);
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Tavern Server Open! Waiting for clients...");
            
            startGameLoop(); 

            // just wait and listen forever
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler pHandler = new ClientHandler(clientSocket, tavernPantry, activeOrders);
                new Thread(pHandler).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // spam every connected terminal at once
    public static void broadcast(String message) {
        for (ClientHandler p : activePlayers) {
            p.sendMessage(message);
        }
    }

    // this generates random chaos in the background
    private static void startGameLoop() {
        new Thread(() -> {
            long lastTickTime = System.currentTimeMillis();
            
            while (true) {
                try {
                    Thread.sleep(1000); // lag here slightly offsets it but who cares 
                    
                    // checking existing orders for timeouts
                    for (OrderTicket ticket : activeOrders.getSnapshot()) {
                        ticket.tickTimer();
                        
                        if (ticket.getTimeLeft() <= 0) {
                            activeOrders.remove(ticket);
                            broadcast(TerminalColors.RED + "\n[ ! ] A customer got fed up and left without their: " 
                                + ticket.getPotion().getName() + "!" + TerminalColors.RESET);
                        } else if (ticket.getTimeLeft() == 10) {
                            broadcast(TerminalColors.YELLOW + "\n[ TICK-TOCK ] Hurry up! The " + ticket.getPotion().getName() 
                                + " is expiring in 10s!" + TerminalColors.RESET);
                        }
                    }

                    // spam a new order roughly every 15 seconds
                    long currentT = System.currentTimeMillis();
                    if (currentT - lastTickTime > 15000) {
                        lastTickTime = currentT;
                        
                        // pull a random recipe
                        String typeStr = ALL_RECIPES[randGen.nextInt(ALL_RECIPES.length)];
                        Potion newOrder = Potion.create(typeStr);
                        
                        OrderTicket ticketObj = new OrderTicket(newOrder, 45); 
                        activeOrders.add(ticketObj);
                        
                        // fancy display
                        broadcast(TerminalColors.GREEN + "\n+================================+" + TerminalColors.RESET);
                        broadcast(TerminalColors.GREEN + " [NEW ORDER] " + newOrder.getName() + "!" + TerminalColors.RESET);
                        broadcast(TerminalColors.YELLOW + " - Recipe: " + newOrder.getRecipeString() + TerminalColors.RESET);
                        broadcast(TerminalColors.CYAN + " - Due in: 45s" + TerminalColors.RESET);
                        broadcast(TerminalColors.GREEN + "+================================+" + TerminalColors.RESET);
                    }
                } catch (InterruptedException e) {
                    System.out.println("Loop interrupted...");
                }
            }
        }).start();
    }
}