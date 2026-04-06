package com.roshambo;

import com.roshambo.models.*; 
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    // standard terminal ansi colors for the ui
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";

    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 6767);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner sc = new Scanner(System.in)) {

            System.out.println(CYAN + BOLD + "Connected to Server! Starting App..." + RESET);

            boolean isRunning = true;
            String myUsername = ""; 

            while (isRunning) {
                System.out.println("\n" + CYAN + "╔══════════════════════════════╗");
                System.out.println("║    " + BOLD + "ROSHAMBO MAIN MENU" + RESET + CYAN + "        ║");
                System.out.println("╚══════════════════════════════╝" + RESET);
                System.out.println("1. Login\n2. Create an Account\n3. Quit");
                System.out.print("> ");
                
                String choice = sc.nextLine();
                
                // basic validation: stop them here instead of wasting server time
                if (!choice.equals("1") && !choice.equals("2") && !choice.equals("3")) {
                    System.out.println(RED + "Invalid choice! Please type 1, 2, or 3." + RESET);
                    continue; 
                }

                if (choice.equals("3")) {
                    out.println("QUIT"); 
                    break; // break the loop and shut down
                }

                System.out.print("Username: ");
                String user = sc.nextLine();
                System.out.print("Password: ");
                String pass = sc.nextLine();

                // make sure they didn't just hit enter on a blank name
                if (user.trim().isEmpty() || pass.trim().isEmpty()) {
                    System.out.println(RED + "Error: Fields cannot be blank." + RESET);
                    continue;
                }

                // format the string command to send over to our server loop
                if (choice.equals("1")) {
                    out.println("LOGIN:" + user + ":" + pass);
                } else {
                    out.println("REGISTER:" + user + ":" + pass);
                }

                // wait to hear back from the database checks
                String serverResponse = in.readLine();
                System.out.println(serverResponse);

                // if login worked, take them straight into the actual game flow
                if (serverResponse.contains("Success") || serverResponse.contains("Account created")) {
                    myUsername = user;
                    boolean loggedIn = true;

                    while(loggedIn) {
                        System.out.println("\n--- [Welcome " + myUsername + "] ---");
                        System.out.println("1. Enter PvP Matchmaking\n2. Log Out");
                        System.out.print("> ");
                        
                        String gameChoice = sc.nextLine();
                        if (gameChoice.equals("2")) {
                            loggedIn = false; // boots them back to main menu loop
                            continue;
                        }
                        if (gameChoice.equals("1")) {
                            System.out.println(YELLOW + "Queuing for a match! Waiting for opponent..." + RESET);
                            out.println("QUEUE_UP");
                            
                            // terminal freezes here intentionally until the server pairs us
                            String matchAck = in.readLine();
                            System.out.println(GREEN + "Match started! " + matchAck + RESET);
                            
                            // we have a match! start calculating the rules completely on the client side.
                            runLocalGameLogic(sc, in, out);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(RED + "Network Connection error. Server offline or unreachable." + RESET);
        }
    }

    // handles the game loop, tracking wins and making all local object calculations
    private static void runLocalGameLogic(Scanner sc, BufferedReader in, PrintWriter out) throws IOException {
        int myWins = 0;
        int oppWins = 0;

        for (int i = 1; i <= 10; i++) {
            System.out.println("\n" + CYAN + "--- ROUND " + i + " ---" + RESET);
            System.out.println("[0] Rock, [1] Paper, [2] Scissors (or type 'quit')");
            System.out.print("> ");

            String input = sc.nextLine();

            // if they forfeit, end it safely
            if (input.equalsIgnoreCase("quit")) {
                System.out.println("You forfeit!");
                out.println("QUIT_MATCH");
                return;
            }

            // convert their raw input text into our actual GameMove classes
            GameMove myMove;
            if (input.equals("0")) { myMove = new Rock(); }
            else if (input.equals("1")) { myMove = new Paper(); }
            else if (input.equals("2")) { myMove = new Scissors(); }
            else {
                System.out.println(RED + "Invalid input format detected. Try again." + RESET);
                i--; // rollback the loop number so we don't skip a round on a typo
                continue; 
            }

            // tell the server what we picked (just plain text, nothing heavy)
            out.println("MOVE:" + myMove.getMoveName());
            System.out.println(YELLOW + "Move locked. Waiting for opponent to pick..." + RESET);

            // terminal waits here until the opponent also picks a move
            String opponentMsg = in.readLine();
            
            // what to do if the other player randomly disconnected
            if(opponentMsg.equals("OPPONENT_QUIT")) {
                System.out.println(RED + "Opponent has left. Match terminated early." + RESET);
                return;
            }

            // figure out what object the other player chose based on what the server sent back
            GameMove oppMove;
            if (opponentMsg.contains("Rock")) oppMove = new Rock();
            else if (opponentMsg.contains("Paper")) oppMove = new Paper();
            else oppMove = new Scissors();

            // use OOP math to see who won so we don't have to deal with hardcoded integers
            int matchMathResult = myMove.compare(oppMove);

            System.out.println(BOLD + "\n[ You played " + myMove.getMoveName() + " | They played " + oppMove.getMoveName() + " ]" + RESET);
            
            // checking output rules
            if (matchMathResult == 0) {
                System.out.println(YELLOW + "Tie encountered! Rules evenly matched." + RESET);
            } else if (matchMathResult == 1) {
                myWins++;
                System.out.println(GREEN + "You claimed the round cleanly: " + myMove.getMoveName() + " defeats " + oppMove.getMoveName() + RESET);
            } else {
                oppWins++;
                System.out.println(RED + "You lost the round: " + oppMove.getMoveName() + " beats " + myMove.getMoveName() + RESET);
            }

            System.out.println("SCORE -> YOU: [" + myWins + "] vs OPP: [" + oppWins + "]");
        }

        // series is done. check variables locally, then only talk to server one more time to record it
        System.out.println("\n" + YELLOW + BOLD + "BEST OF TEN SERIES HAS COMPLETED" + RESET);
        
        if (myWins > oppWins) {
            System.out.println(GREEN + "YOU SECURED THE MATCH VICTORY! Pushing scores upward..." + RESET);
            out.println("SAVE_MY_WIN"); 
        } else if (myWins < oppWins) {
            System.out.println(RED + "Opponent mathematically outperformed you. Sorry." + RESET);
            out.println("SAVE_MY_LOSS");
        } else {
            System.out.println(CYAN + "Total Tiebreaker! It's a draw." + RESET);
            out.println("SAVE_MY_TIE"); 
        }
    }
}