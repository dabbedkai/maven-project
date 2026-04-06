package com.roshambo.services;

import com.roshambo.models.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.Type;

public class RoshamboServer {

    private static final String JSON_FILE = "players.json";
    private static ArrayList<Player> users = new ArrayList<>();
    
    // instantiate gson object
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // holding slot for the player waiting for a matchmaking game
    private static ClientHandler waitingPlayer = null;

    public static void main(String[] args) {
        int port = 6767;
        loadUsers();

        try {
            ServerSocket server = new ServerSocket(port);
            System.out.println("Roshambo Matchmaking Server Online! Listening on port " + port);

            while (true) {
                Socket client = server.accept();
                System.out.println("New player connected: " + client.getInetAddress().getHostAddress());
                
                ClientHandler ch = new ClientHandler(client);
                Thread thread = new Thread(ch);
                thread.start();
            }
        } catch (Exception e) {
            System.out.println("Server exception: ");
            e.printStackTrace();
        }
    }


    public static synchronized void saveUsers() {
        try {
            FileWriter writer = new FileWriter(JSON_FILE);
            gson.toJson(users, writer); // automatically writes the entire array as json to the file
            writer.close();
        } catch (IOException e) {
            System.out.println("Error saving user state locally.");
        }
    }

    public static synchronized void loadUsers() {
        File f = new File(JSON_FILE);
        if (!f.exists()) {
            return;
        }

        try {
            FileReader reader = new FileReader(f);
            Type userListType = new TypeToken<ArrayList<Player>>(){}.getType();
            ArrayList<Player> loadedUsers = gson.fromJson(reader, userListType);
            
            if (loadedUsers != null) {
                users = loadedUsers;
            }
            reader.close();
        } catch (Exception e) {
            System.out.println("Failed to read JSON Database.");
        }
    }
    
    // registeruser checks for duplicate usernames and adds new users to the list, then saves to JSON
    public static synchronized boolean registerUser(String username, String password) {
        for (int i = 0; i < users.size(); i++) {
            Player u = users.get(i);
            if (u.getUsername().equalsIgnoreCase(username)) {
                return false; // prevents duplicates
            }
        }
        
        Player newP = new Player(username, password, 0, 0);
        users.add(newP);
        saveUsers();
        
        return true;
    }

    // authenticate checks if the provided credentials match any existing user and returns that user object if successful
    public static synchronized Player authenticate(String username, String password) {
        for (int i = 0; i < users.size(); i++) {
            Player u = users.get(i);
            if (u.getUsername().equalsIgnoreCase(username) && u.getPassword().equals(password)) {
                return u;
            }
        }
        return null;
    }

    // updatescore modifies a user's score and persists the change to the JSON file
    public static synchronized void updatePlayerStats(Player user, boolean isWinner) {
        // increase total matches played for this user no matter what
        user.setMatchesPlayed(user.getMatchesPlayed() + 1);
        
        // increase their score (win tally) only if they actually won
        if (isWinner == true) {
            user.incrementScore(); // securely updating through model rules 
        }
        
        saveUsers();
    }
    
    public static synchronized void printLeaderboard(ClientHandler p1, ClientHandler p2) {
        List<Player> sortedUsers = new ArrayList<>(users);
        
        // loop sorting to order by win rate percentage
        for (int i = 0; i < sortedUsers.size(); i++) {
            for (int j = i + 1; j < sortedUsers.size(); j++) {
                
                double winRateA = sortedUsers.get(i).getWinRate();
                double winRateB = sortedUsers.get(j).getWinRate();
                
                // sort highest to lowest win rate
                if (winRateA < winRateB) {
                    Player temp = sortedUsers.get(i);
                    sortedUsers.set(i, sortedUsers.get(j));
                    sortedUsers.set(j, temp);
                } 
                // if there is a tie, let the player with more actual games played take higher rank
                else if (winRateA == winRateB) {
                    int gamesA = sortedUsers.get(i).getMatchesPlayed();
                    int gamesB = sortedUsers.get(j).getMatchesPlayed();
                    
                    if (gamesA < gamesB) {
                        Player temp = sortedUsers.get(i);
                        sortedUsers.set(i, sortedUsers.get(j));
                        sortedUsers.set(j, temp);
                    }
                }
            }
        }
        
        String lb = "\n=== GLOBAL LEADERBOARD (WIN RATE) ===\n";
        int rank = 1;
        
        for (int i = 0; i < sortedUsers.size(); i++) {
            Player p = sortedUsers.get(i);
            
            // round the win rate to 1 decimal place for display purposes
            double wrRaw = p.getWinRate();
            double roundedWr = Math.round(wrRaw * 10.0) / 10.0;
            
            lb += rank + ". " + p.getUsername() + " - " + roundedWr + "% (" + p.getScore() + "W / " + p.getMatchesPlayed() + " Total)\n";
            rank++;
        }
        
        lb += "=====================================\n";
        
        p1.out.println(lb);
        p2.out.println(lb);
    }

    // client handler class manages all interactions with a connected client, including login, registration, matchmaking, and gameplay
    private static class ClientHandler implements Runnable {
        private Socket socket;
        public BufferedReader in;
        public PrintWriter out;
        public Player loggedInUser = null;

        public ClientHandler(Socket s) {
            this.socket = s;
        }

        // main run method for the client handler thread
        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("Connection Established!");

                boolean running = true;
                while (running == true) {
                    out.println("\n--- ROSHAMBO PVP SERVER ---");
                    out.println("1. Login\n2. Create an Account\n3. Quit");
                    out.println("Option: ");
                    out.println("INPUT_REQUIRED");

                    String choice = in.readLine();
                    if (choice == null) {
                        break;
                    }

                    switch (choice) {
                        case "1":
                            boolean logged = executeLogin();
                            if (logged == true) {
                                runMainMenu();
                            }
                            break;
                        case "2":
                            executeRegister();
                            break;
                        case "3":
                            out.println("QUIT_CLIENT");
                            running = false;
                            break;
                        default:
                            out.println("Invalid selection.");
                    }
                }
                socket.close();

            } catch (IOException e) {
                System.out.println("A player dropped connection.");
            } finally {
                synchronized (RoshamboServer.class) {
                    if (waitingPlayer == this) {
                        waitingPlayer = null;
                    }
                }
            }
        }

        // executelogin handles the login flow, prompting for credentials and authenticating against the user list
        private boolean executeLogin() throws IOException {
            out.println("Username: ");
            out.println("INPUT_REQUIRED");
            String username = in.readLine();

            out.println("Password: ");
            out.println("INPUT_REQUIRED");
            String pw = in.readLine();
            
            // basic input validation blocking blank spaces preventing server array corruption
            if (username == null || username.trim().isEmpty() || pw == null || pw.trim().isEmpty()) {
                out.println("Error: username or password cannot be completely empty.");
                return false;
            }

            Player authResult = authenticate(username, pw);
            if (authResult != null) {
                loggedInUser = authResult;
                out.println("Login Successful!");
                return true;
            }
            
            out.println("Incorrect login credentials.");
            return false;
        }

        // executetegister manages the account creation process, ensuring unique usernames and adding new users to the system
        private void executeRegister() throws IOException {
            out.println("Enter New Username: ");
            out.println("INPUT_REQUIRED");
            String username = in.readLine();
            
            out.println("Enter New Password: ");
            out.println("INPUT_REQUIRED");
            String pass = in.readLine();
            
            // standard school friendly string checking verifying fields aren't completely blank spaces 
            if (username == null || username.trim().isEmpty() || pass == null || pass.trim().isEmpty()) {
                out.println("Registration failed! Please use real characters for your account.");
                return;
            }
            
            if(registerUser(username, pass)) {
                out.println("Account created! Please log in.");
            } else {
                out.println("That username is already taken!");
            }
        }

        // runmainmenu displays the main menu for logged-in users, allowing them to find matches or log out
        private void runMainMenu() throws IOException {
            while (loggedInUser != null) {
                double dispRateRaw = loggedInUser.getWinRate();
                double dispRateRound = Math.round(dispRateRaw * 10.0) / 10.0;
                
                out.println("\n[ WELCOME " + loggedInUser.getUsername().toUpperCase() + " | WIN RATE: " + dispRateRound + "% ]");
                out.println("1. Find Match (PvP)\n2. Logout");
                out.println("Choice: ");
                out.println("INPUT_REQUIRED");

                String select = in.readLine();
                if(select == null) return;
                
                if (select.equals("1")) {
                    out.println("Entering matchmaking queue...");
                    findOpponent();
                } else if (select.equals("2")) {
                    out.println("Logging out.");
                    loggedInUser = null;
                } else {
                    out.println("Invalid choice.");
                }
            }
        }

        // findopponent implements a simple matchmaking system where the first player waits and the second player triggers the match start, then runs the game logic in a new thread
        private void findOpponent() {
            ClientHandler opponent = null;

            synchronized (RoshamboServer.class) {
                if (waitingPlayer == null) {
                    waitingPlayer = this;
                } else {
                    opponent = waitingPlayer;
                    waitingPlayer = null;
                }
            }

            if (opponent != null) {
                out.println("Match found! You are playing against " + opponent.loggedInUser.getUsername());
                opponent.out.println("Match found! You are playing against " + this.loggedInUser.getUsername());

                // fires off strictly mapped state encapsulation instead of legacy math calls 
                GameSession match = new GameSession(opponent, this);
                match.run(); 

            } else {
                try {
                    out.println("Waiting for an opponent to join...");
                    synchronized (this) {
                        this.wait(); 
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // handles logic locking completely stopping data pollution out from core threading logic 
    private static class GameSession implements Runnable {
        private ClientHandler p1;
        private ClientHandler p2;

        public GameSession(ClientHandler p1, ClientHandler p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        private void broadcast(String msg) {
            p1.out.println(msg);
            p2.out.println(msg);
        }

        private void notifyPlayerQuit() {
            broadcast("Someone has surrendered/left.");
        }

        // securely checks and pushes plain texts converting logic models polymorphism requirements gracefully 
        private GameMove parseMove(String input) {
            if (input.equals("0")) { return new Rock(); }
            if (input.equals("1")) { return new Paper(); }
            if (input.equals("2")) { return new Scissors(); }
            return null; // implicitly manages strict format validation dropping out errors nicely 
        }
        
        // main game loop for the roshambo game, handling move input, validation, result evaluation, score updates, and showing results to both players
        @Override
        public void run() {
            try {
                int p1Wins = 0;
                int p2Wins = 0;

                broadcast("=== STARTING 10 ROUND MATCH ===");

                // game loop continues until a player quits or disconnects, handling move input and result evaluation each round
                for(int round = 1; round <= 10; round++) {
                    broadcast("\n-- ROUND " + round + " --");
                    p2.out.println("Waiting for Player 1 to make a move...");
                    p1.out.println("\nYOUR_TURN_RPS");

                    String move1 = p1.in.readLine();
                    if (move1 == null || move1.equalsIgnoreCase("quit")) {
                        notifyPlayerQuit(); 
                        break;
                    }

                    p1.out.println("Move locked. Waiting for Player 2...");
                    p2.out.println("\nYOUR_TURN_RPS"); 

                    String move2 = p2.in.readLine();
                    if (move2 == null || move2.equalsIgnoreCase("quit")) {
                        notifyPlayerQuit(); 
                        break;
                    }

                    move1 = move1.trim().toLowerCase();
                    move2 = move2.trim().toLowerCase();
                    
                    GameMove m1obj = parseMove(move1);
                    GameMove m2obj = parseMove(move2);

                    if (m1obj == null || m2obj == null) {
                        broadcast("Invalid inputs detected. Round nullified and repeating round.");
                        round--; // retry this round gracefully trapping incorrect terminal presses
                        continue;
                    }
                    
                    // assign dynamically parsed choices deeply internally checking parameters
                    p1.loggedInUser.setCurrentMove(m1obj);
                    p2.loggedInUser.setCurrentMove(m2obj);

                    // abstracts out mathematics utilizing polymorphic checking naturally passing states 
                    int compRes = p1.loggedInUser.getCurrentMove().compare(p2.loggedInUser.getCurrentMove());
                    
                    String act1 = p1.loggedInUser.getCurrentMove().getMoveName();
                    String act2 = p2.loggedInUser.getCurrentMove().getMoveName();

                    broadcast("--- ROUND RESULTS ---");
                    
                    if (compRes == 0) {
                        broadcast("It's a Tie! Both picked " + act1);
                    } else if (compRes == 1) {
                        p1Wins++;
                        p1.out.println("You won the round! " + act1 + " beats " + act2);
                        p2.out.println("You lost the round. " + act1 + " beats " + act2);
                    } else if (compRes == -1) {
                        p2Wins++;
                        p2.out.println("You won the round! " + act2 + " beats " + act1);
                        p1.out.println("You lost the round. " + act2 + " beats " + act1);
                    }

                    broadcast("CURRENT SCORE: " + p1.loggedInUser.getUsername() + "[" + p1Wins + "] vs " + 
                              p2.loggedInUser.getUsername() + "[" + p2Wins + "]");
                }

                String overallWinner;
                
                if (p1Wins > p2Wins) {
                    overallWinner = p1.loggedInUser.getUsername();
                    updatePlayerStats(p1.loggedInUser, true); 
                    updatePlayerStats(p2.loggedInUser, false);
                } else if (p2Wins > p1Wins) {
                    overallWinner = p2.loggedInUser.getUsername();
                    updatePlayerStats(p2.loggedInUser, true);
                    updatePlayerStats(p1.loggedInUser, false);
                } else {
                    overallWinner = "Tie - No overall winner points awarded.";
                    // it is a tie, so they both get +1 match played but neither gets +1 win tally
                    updatePlayerStats(p1.loggedInUser, false);
                    updatePlayerStats(p2.loggedInUser, false);
                }

                RoshamboResult matchFinalRecord = new RoshamboResult(
                    p1.loggedInUser.getUsername(),
                    p2.loggedInUser.getUsername(),
                    p1Wins, 
                    p2Wins,
                    overallWinner
                );

                broadcast("\n===== THE 10 ROUND MATCH IS OVER! =====");
                broadcast(matchFinalRecord.getFormattedSummary());
                
                // displays global json scoreboard  
                printLeaderboard(p1, p2); 

            } catch (Exception e) {
                System.out.println("Connection failed mid-game.");
            } finally {
                p1.out.println("Leaving Game Room. Sending you back to Menu.");
                p2.out.println("Leaving Game Room. Sending you back to Menu.");
                
                synchronized(p1) { 
                    p1.notify(); 
                }
            }
        }
    }
}