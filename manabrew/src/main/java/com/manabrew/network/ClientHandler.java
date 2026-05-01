package com.manabrew.network;

import com.manabrew.inventory.*;
import com.manabrew.model.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private Pantry pantry;
    private StorageBunker<OrderTicket> orders;
    private PrintWriter out;
    private String playerName;

    public ClientHandler(Socket socket, Pantry pantry, StorageBunker<OrderTicket> orders) {
        this.socket = socket;
        this.pantry = pantry;
        this.orders = orders;
    }

    public void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            out.println(TerminalColors.CYAN + "Welcome to ManaBrew Co-Op Kitchen!" + TerminalColors.RESET);
            out.println("What is your alchemist name?");
            
            // waits here until the player hits enter
            playerName = in.readLine();
            
            // now we plug them into the chaos of the active kitchen
            Server.addPlayer(this);
            Server.broadcast(TerminalColors.YELLOW + "[ LOGIN ] " + playerName + " entered the kitchen!" + TerminalColors.RESET);
            out.println("Syntax: brew <ingredient1>, <ingredient2> (e.g. 'brew dragon scale, fairy dust')\n");

            String input;
            while ((input = in.readLine()) != null) {
                if (input.equals("quit")) break;
                
                if (input.startsWith("brew ")) {
                    String reqString = input.substring(5).trim();
                    String[] userIngredients = reqString.split(",");
                    
                    for (int i = 0; i < userIngredients.length; i++) {
                        userIngredients[i] = userIngredients[i].trim().toLowerCase();
                    }
                    
                    Arrays.sort(userIngredients);
                    
                    OrderTicket targetOrder = null;
                    for (OrderTicket ticket : orders.getSnapshot()) {
                        String[] targetRecipe = extractNames(ticket.getPotion().getRecipe());
                        Arrays.sort(targetRecipe);
                        
                        if (Arrays.equals(userIngredients, targetRecipe)) {
                            targetOrder = ticket;
                            break; 
                        }
                    }
                    
                    if (targetOrder != null) {
                        if (userIngredients.length == 2 && Brewable.isVolatile(userIngredients[0], userIngredients[1])) {
                            out.println(TerminalColors.RED + "[ ! ] WARNING: Handling highly volatile mixture!" + TerminalColors.RESET);
                        }

                        if (pantry.takeIngredients(userIngredients)) {
                            int brewTime = targetOrder.getPotion().calculateBrewTime(targetOrder.getPotion().getTier());
                            Server.broadcast(TerminalColors.BLUE + "[ ACTIVE ] " + playerName + " is working on " 
                                + targetOrder.getPotion().getName() + "!" + TerminalColors.RESET);
                                
                            Thread.sleep(brewTime * 1000L);

                            if (orders.remove(targetOrder)) {
                                Server.addGold(targetOrder.getPotion().getPrice());
                                Server.broadcast(TerminalColors.GREEN + "[ $$$ ] YES! " + playerName + " successfully delivered " 
                                    + targetOrder.getPotion().getName() + "! (Gold: " + Server.totalGold + ")" + TerminalColors.RESET);
                            } else {
                                out.println(TerminalColors.RED + "Oh no! the ticket expired while you were brewing..." + TerminalColors.RESET);
                            }
                        } else {
                            out.println(TerminalColors.RED + "Not enough raw stock in the pantry!" + TerminalColors.RESET);
                        }
                    } else {
                        out.println(TerminalColors.RED + "[ X ] Unknown combination OR no active order needs this combination right now!" + TerminalColors.RESET);
                    }
                }
            }
        } catch (Exception e) {
            Server.broadcast(TerminalColors.RED + "[ DISCONNECT ] " + playerName + " dropped out!" + TerminalColors.RESET);
        }
    }

    private String[] extractNames(Ingredient[] ings) {
        String[] arr = new String[ings.length];
        for(int i = 0; i < ings.length; i++) {
            arr[i] = ings[i].getName().toLowerCase();
        }
        return arr;
    }
}