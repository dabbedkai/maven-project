package com.roshambo;

// bringing in models implicitly allowing client logic calculations securely formatting objects purely
import com.roshambo.models.*; 
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    // ansi colors for the client-side prompts visually routing texts mapping terminal blocks nicely
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

            // boolean toggle bridging process closure naturally terminating process loop cleanly
            boolean isRunning = true;
            String myUsername = ""; 

            while (isRunning) {
                System.out.println("\n" + CYAN + "╔══════════════════════════════╗");
                System.out.println("║    " + BOLD + "ROSHAMBO MAIN MENU" + RESET + CYAN + "        ║");
                System.out.println("╚══════════════════════════════╝" + RESET);
                System.out.println("1. Login\n2. Create an Account\n3. Quit");
                System.out.print("> ");
                
                String choice = sc.nextLine();
                
                // client-side validation instantly blocking bad menu choices naturally saving server processing bandwidth gracefully
                if (!choice.equals("1") && !choice.equals("2") && !choice.equals("3")) {
                    System.out.println(RED + "Invalid choice! Please type 1, 2, or 3." + RESET);
                    continue; 
                }

                if (choice.equals("3")) {
                    out.println("QUIT"); 
                    break;
                }

                System.out.print("Username: ");
                String user = sc.nextLine();
                System.out.print("Password: ");
                String pass = sc.nextLine();

                // checks for empty string pollution before hitting the database implicitly managing strict constraints locally 
                if (user.trim().isEmpty() || pass.trim().isEmpty()) {
                    System.out.println(RED + "Error: Fields cannot be blank spaces." + RESET);
                    continue;
                }

                // packages standard credentials routing commands tightly stringing together payloads seamlessly 
                if (choice.equals("1")) {
                    out.println("LOGIN:" + user + ":" + pass);
                } else {
                    out.println("REGISTER:" + user + ":" + pass);
                }

                // barricades console waiting securely resolving response structures sent exclusively by database checks
                String serverResponse = in.readLine();
                System.out.println(serverResponse);

                // automatically mapping player flow bridging menus dynamically without forcing reconnects 
                if (serverResponse.contains("Success") || serverResponse.contains("Account created")) {
                    myUsername = user;
                    boolean loggedIn = true;

                    while(loggedIn) {
                        System.out.println("\n--- [Welcome " + myUsername + "] ---");
                        System.out.println("1. Enter PvP Matchmaking\n2. Log Out");
                        System.out.print("> ");
                        
                        String gameChoice = sc.nextLine();
                        if (gameChoice.equals("2")) {
                            loggedIn = false;
                            continue;
                        }
                        if (gameChoice.equals("1")) {
                            System.out.println(YELLOW + "Queuing for a match! Waiting for opponent..." + RESET);
                            out.println("QUEUE_UP");
                            
                            // stalls inherently holding terminal locks anticipating human matched events completely natively
                            String matchAck = in.readLine();
                            System.out.println(GREEN + "Match started! " + matchAck + RESET);
                            
                            // launches internal local mapping exclusively wrapping rule evaluation perfectly locally
                            runLocalGameLogic(sc, in, out);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(RED + "Network Connection error. Server offline or unreachable." + RESET);
        }
    }

    // handles mathematics calculating local variables dynamically processing abstractions implicitly
    private static void runLocalGameLogic(Scanner sc, BufferedReader in, PrintWriter out) throws IOException {
        int myWins = 0;
        int oppWins = 0;

        for (int i = 1; i <= 10; i++) {
            System.out.println("\n" + CYAN + "--- ROUND " + i + " ---" + RESET);
            System.out.println("[0] Rock, [1] Paper, [2] Scissors (or type 'quit')");
            System.out.print("> ");

            String input = sc.nextLine();

            // securely catching quit escapes before instantiating objects saving arrays naturally
            if (input.equalsIgnoreCase("quit")) {
                System.out.println("You forfeit!");
                out.println("QUIT_MATCH");
                return;
            }

            // utilizes abstractions polymorphically translating generic choices cleanly
            GameMove myMove;
            if (input.equals("0")) { myMove = new Rock(); }
            else if (input.equals("1")) { myMove = new Paper(); }
            else if (input.equals("2")) { myMove = new Scissors(); }
            else {
                System.out.println(RED + "Invalid input format detected naturally catching out errors." + RESET);
                // reverts loop forcing safe string completion internally gracefully
                i--; 
                continue; 
            }

            // transmits only textual identities masking process overhead strictly bridging network correctly 
            out.println("MOVE:" + myMove.getMoveName());
            System.out.println(YELLOW + "Move locked. Waiting for opponent to pick..." + RESET);

            // waiting out packet reception dropping through responses asynchronously natively
            String opponentMsg = in.readLine();
            
            // handling disconnects randomly completely dropping broken iterations nicely
            if(opponentMsg.equals("OPPONENT_QUIT")) {
                System.out.println(RED + "Opponent has left. Match terminated properly." + RESET);
                return;
            }

            // builds opposite opposing logic instances explicitly connecting model functionality symmetrically 
            GameMove oppMove;
            if (opponentMsg.contains("Rock")) oppMove = new Rock();
            else if (opponentMsg.contains("Paper")) oppMove = new Paper();
            else oppMove = new Scissors();

            // compares instances checking internal rule classes completely eliminating ints purely objectively 
            int matchMathResult = myMove.compare(oppMove);

            System.out.println(BOLD + "\n[ You played " + myMove.getMoveName() + " | They played " + oppMove.getMoveName() + " ]" + RESET);
            
            // handles mathematical mappings displaying match result tags appropriately coloring screens heavily
            if (matchMathResult == 0) {
                System.out.println(YELLOW + "Tie encountered. Rules evenly matched out." + RESET);
            } else if (matchMathResult == 1) {
                myWins++;
                System.out.println(GREEN + "You claimed the round cleanly " + myMove.getMoveName() + " defeats " + oppMove.getMoveName() + RESET);
            } else {
                oppWins++;
                System.out.println(RED + "Round naturally defeated by logic constraints " + oppMove.getMoveName() + " beats " + myMove.getMoveName() + RESET);
            }

            System.out.println("SCORE -> YOU: [" + myWins + "] vs OPP: [" + oppWins + "]");
        }

        // wraps series logically assessing values directly posting local metrics up remotely flawlessly 
        System.out.println("\n" + YELLOW + BOLD + "BEST OF TEN SERIES HAS COMPLETED" + RESET);
        
        if (myWins > oppWins) {
            System.out.println(GREEN + "YOU SECURED THE MATCH VICTORY natively adding score tallies!" + RESET);
            out.println("SAVE_MY_WIN"); 
        } else if (myWins < oppWins) {
            System.out.println(RED + "Opponent mathematically outperformed logic cleanly." + RESET);
            out.println("SAVE_MY_LOSS");
        } else {
            System.out.println(CYAN + "Standstill tie matching completely." + RESET);
            out.println("SAVE_MY_TIE"); 
        }
    }
}