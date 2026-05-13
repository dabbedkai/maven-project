package com.manabrew.network;

import com.manabrew.inventory.*;
import com.manabrew.model.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket myConnection;
    private PrintWriter outBuffer;
    private String playerName;
    
    private GameRoom myRoom = null; 

    public ClientHandler(Socket s) {
        this.myConnection = s;
    }

    public void sendMessage(String m) {
        if (outBuffer != null) {
            outBuffer.println(m);
        }
    }
    
    public String getUsername() {
        return playerName;
    }

    @Override
    public void run() {
        try {
            outBuffer = new PrintWriter(myConnection.getOutputStream(), true);
            BufferedReader inReader = new BufferedReader(new InputStreamReader(myConnection.getInputStream()));
            
            // welcome text and username binding
            outBuffer.println(TerminalColors.CYAN + "\n~~~ Welcome to ManaBrew Multiplayer ~~" + TerminalColors.RESET);
            outBuffer.println("Enter your alchemist handle:");
            playerName = inReader.readLine();
            
            outBuffer.println(TerminalColors.YELLOW + "Logged in as " + playerName + "." + TerminalColors.RESET);
            outBuffer.println("Type 'join <code>' to enter or create a lobby (e.g., 'join 1234').\n");

            String cmdLine;
            while ((cmdLine = inReader.readLine()) != null) {
                if (cmdLine.equalsIgnoreCase("quit")) break;
                
                // joining lobby
                if (cmdLine.toLowerCase().startsWith("join ")) {
                    String code = cmdLine.split(" ")[1];
                    myRoom = Server.getOrCreateRoom(code);
                    myRoom.addPlayer(this, playerName);
                }
                
                // round start
                else if (cmdLine.equalsIgnoreCase("start") && myRoom != null) {
                    if (myRoom.hostPlayer.equals(playerName)) {
                        myRoom.startRound();
                    } else {
                        outBuffer.println(TerminalColors.RED + "Only the host (" + myRoom.hostPlayer + ") can start the round!" + TerminalColors.RESET);
                    }
                }

                // shopping phase command
                else if (cmdLine.toLowerCase().startsWith("shop ") && myRoom != null) {
                    if (myRoom.isShopPhase) {
                        String item = cmdLine.substring(5).trim();
                        if (myRoom.sharedVaultGold >= 10) { 
                            myRoom.sharedVaultGold -= 10; // Shared resource deduction!
                            myRoom.roomPantry.addStock(item, 1); 
                            myRoom.broadcast(TerminalColors.GREEN + playerName + " bought 1x " + item + " for the team!" + TerminalColors.RESET);
                        } else {
                            outBuffer.println(TerminalColors.RED + "The team vault is broke! You need 10g." + TerminalColors.RESET);
                        }
                    } else {
                        outBuffer.println(TerminalColors.RED + "The shop is closed during an active round!" + TerminalColors.RESET);
                    }
                }

                // ticket locking
                else if (cmdLine.toLowerCase().startsWith("claim ") && myRoom != null && !myRoom.isShopPhase) {
                    String wantedName = cmdLine.substring(6).trim();
                    boolean found = false;
                    
                    for (OrderTicket ticket : myRoom.orders.getSnapshot()) {
                        if (ticket.getPotion().getName().equalsIgnoreCase(wantedName)) {
                            found = true;
                            // lock attempt
                            if (ticket.claim(playerName)) {
                                myRoom.broadcast(TerminalColors.BLUE + playerName + " LOCKED the ticket for: " + wantedName + "!" + TerminalColors.RESET);
                            } else {
                                outBuffer.println(TerminalColors.RED + "Too slow! Someone else (" + ticket.getClaimedBy() + ") is doing that order." + TerminalColors.RESET);
                            }
                            break;
                        }
                    }
                    if (!found) outBuffer.println(TerminalColors.RED + "Order not found. Check your spelling." + TerminalColors.RESET);
                }

                // active round
                else if (cmdLine.toLowerCase().startsWith("brew ") && myRoom != null && !myRoom.isShopPhase) {
                    String cleanArgs = cmdLine.substring(5).trim();
                    String[] userGivenIngs = cleanArgs.split(",");
                    
                    // sorting user input exactly like we did before
                    for (int k = 0; k < userGivenIngs.length; k++) {
                        userGivenIngs[k] = userGivenIngs[k].trim().toLowerCase();
                    }
                    Arrays.sort(userGivenIngs);
                    
                    // checking order history for a match
                    OrderTicket targetOrder = null;
                    for (OrderTicket possibleTk : myRoom.orders.getSnapshot()) {
                        String[] requiredIngs = dumpNames(possibleTk.getPotion().getRecipe());
                        Arrays.sort(requiredIngs);
                        
                        if (Arrays.equals(userGivenIngs, requiredIngs)) {
                            targetOrder = possibleTk;
                            break; 
                        }
                    }
                    
                    if (targetOrder != null) {
                        if (playerName.equals(targetOrder.getClaimedBy())) {
                            
                            // explosion utility
                            if (userGivenIngs.length == 2 && Brewable.isVolatile(userGivenIngs[0], userGivenIngs[1])) {
                                outBuffer.println(TerminalColors.RED + "CAUTION: Volatile components interacting!" + TerminalColors.RESET);
                            }

                            // deduct ingredients from pantry
                            if (myRoom.roomPantry.takeIngredients(userGivenIngs)) {
                                myRoom.broadcast(TerminalColors.BLUE + "[ACTIVE] " + playerName + " is furiously mixing the " + targetOrder.getPotion().getName() + "..." + TerminalColors.RESET);
                                
                                int secs = targetOrder.getPotion().getTier() * 2; 

                                outBuffer.print(TerminalColors.CYAN + "[");
                                for(int c = 0; c < 10; c++) {
                                    outBuffer.print("■"); 
                                    outBuffer.flush(); 
                                    Thread.sleep((secs * 100)); 
                                }
                                outBuffer.println("] DONE!" + TerminalColors.RESET);

                                if (myRoom.orders.remove(targetOrder)) {
                                    myRoom.sharedVaultGold += targetOrder.getPotion().getPrice();

                                    Server.potionsDelivered++;
                                    Server.totalGold += targetOrder.getPotion().getPrice(); 
                                    
                                    myRoom.broadcast(TerminalColors.GREEN + "$$ YES! " + playerName + " delivered it! Vault: " + myRoom.sharedVaultGold + "g." + TerminalColors.RESET);
                                }
                            } else {
                                outBuffer.println(TerminalColors.RED + "Not enough stock in the pantry! Wait for the shopping phase!" + TerminalColors.RESET);
                            }
                        } else {
                            outBuffer.println(TerminalColors.RED + "You must lock the order with 'claim " + targetOrder.getPotion().getName() + "' first!" + TerminalColors.RESET);
                        }
                    } else {
                        outBuffer.println(TerminalColors.RED + "Bad combination or no active order matches those ingredients." + TerminalColors.RESET);
                    }
                }
            }
        } catch (Exception ex) {
            if (myRoom != null) {
                myRoom.handleDisconnect(this);
            }
            System.out.println(TerminalColors.YELLOW + "Network Log: " + playerName + " left." + TerminalColors.RESET);
        }
    }

    private String[] dumpNames(Ingredient[] itemsArr) {
        String[] txts = new String[itemsArr.length];
        for(int x = 0; x < itemsArr.length; x++) {
            txts[x] = itemsArr[x].getName().toLowerCase();
        }
        return txts;
    }
}