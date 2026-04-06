package com.roshambo.services;

import com.roshambo.models.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.Type;

// simple tracker to keep an eye on what step the player is currently on
enum Status {
    BROWSING_MENUS, WAITING_IN_QUEUE, IN_MATCH
}

// standard data object so we don't need threading. stores everything we need for one user.
class ClientTracker {
    Socket socket;
    BufferedReader in;
    PrintWriter out;
    Status status = Status.BROWSING_MENUS;
    Player userProfile = null;
    
    // lets us link two different users directly to each other when game time starts
    ClientTracker activeOpponent = null;
    String latestMoveReady = null;

    public ClientTracker(Socket socket) throws IOException {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }
}

public class RoshamboServer {
    private static final String JSON = "players.json";
    private static ArrayList<Player> database = new ArrayList<>();
    
    // built-in gson configuration using their standard templates
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create(); 

    public static void main(String[] args) {
        loadData();
        List<ClientTracker> connectedDevices = new ArrayList<>();

        try {
            try (ServerSocket serverSocket = new ServerSocket(6767)) {
                // this is the trick to removing threads. setting timeout stops accept() from freezing forever!
                serverSocket.setSoTimeout(15); 
                System.out.println("Single-loop hub server running cleanly on Port 6767");

                while (true) {
                    // 1. quickly check if there is a new incoming connection and add them to the list
                    try {
                        Socket tempSoc = serverSocket.accept();
                        connectedDevices.add(new ClientTracker(tempSoc));
                        System.out.println("A user has connected!");
                    } catch (SocketTimeoutException ignored) { } 
                    
                    // 2. rapidly iterate through all currently online people checking for messages
                    for (int i = 0; i < connectedDevices.size(); i++) {
                        ClientTracker c = connectedDevices.get(i);
                        try {
                            // if they actually hit the enter key recently, pull the data string in
                            if (c.in.ready()) {
                                String msg = c.in.readLine();
                                if (msg != null) handleRequest(c, msg); 
                            }
                        } catch (IOException dropErr) {
                            // handles them pulling out the internet plug or hard crashing
                            if(c.activeOpponent != null) c.activeOpponent.out.println("OPPONENT_QUIT");
                            connectedDevices.remove(c); 
                            i--; // shifts iteration index left safely so loop doesn't crash on removals
                        }
                    }

                    // 3. check queue lists continuously, hooking up match partners
                    pairQueuedPlayers(connectedDevices);

                    // 4. chill out cpu limits a bit so the computer fan doesn't ramp up hard
                    Thread.sleep(10);
                }
            }
        } catch (Exception fatal) { 
            fatal.printStackTrace(); 
        }
    }

    // parsing through string tags matching requests with basic string operations 
    public static void handleRequest(ClientTracker c, String msg) {
        String[] dataParts = msg.split(":"); 

        switch (dataParts[0]) {
            case "LOGIN":
                Player p = findAcc(dataParts[1], dataParts[2]);
                if (p != null) {
                    c.userProfile = p;
                    c.out.println("\u001B[32mSuccess! Welcome back, " + p.getUsername() + "!\u001B[0m");
                } else {
                    c.out.println("\u001B[31mLogin failed. Incorrect user or pass.\u001B[0m");
                }
                break;
                
            case "REGISTER":
                // gracefully handle repeats so they don't break files
                if (findAcc(dataParts[1], dataParts[2]) == null) {
                    Player n = new Player(dataParts[1], dataParts[2], 0, 0);
                    database.add(n);
                    saveData();
                    c.out.println("\u001B[32mCreated cleanly. Account made successfully!\u001B[0m");
                } else {
                    c.out.println("\u001B[31mRegistration blocked. User exists.\u001B[0m");
                }
                break;

            case "QUEUE_UP":
                c.status = Status.WAITING_IN_QUEUE;
                break;

            case "QUIT_MATCH": 
            case "QUIT": 
                // tells opponent process that the current link is dropping and wipes target clean
                if (c.activeOpponent != null) {
                    c.activeOpponent.out.println("OPPONENT_QUIT");
                    c.activeOpponent.activeOpponent = null; 
                }
                break;

            case "MOVE":
                // saves text waiting temporarily until opponent follows up 
                c.latestMoveReady = dataParts[1]; 
                
                // wait... did they BOTH hit enter? push it forward natively without syncing errors.
                if (c.activeOpponent != null && c.activeOpponent.latestMoveReady != null) {
                    c.out.println("PLAYED:" + c.activeOpponent.latestMoveReady);
                    c.activeOpponent.out.println("PLAYED:" + c.latestMoveReady);
                    
                    // clean buffers prepping logic seamlessly for next turn iterations.
                    c.latestMoveReady = null; 
                    c.activeOpponent.latestMoveReady = null; 
                }
                break;
                
            // pushes wins updating data automatically
            case "SAVE_MY_WIN":
                c.userProfile.setMatchesPlayed(c.userProfile.getMatchesPlayed() + 1);
                c.userProfile.incrementScore();
                saveData(); 
                c.status = Status.BROWSING_MENUS;
                break;

            case "SAVE_MY_LOSS":
            case "SAVE_MY_TIE":
                // losing or tie increments played, just zero raw points. 
                c.userProfile.setMatchesPlayed(c.userProfile.getMatchesPlayed() + 1);
                saveData(); 
                c.status = Status.BROWSING_MENUS;
                break;
        }
    }

    // scans currently linked arrays pairing whoever hit 1 basically concurrently naturally mapping things
    public static void pairQueuedPlayers(List<ClientTracker> devices) {
        ClientTracker waitingPlayer1 = null;

        for (int i = 0; i < devices.size(); i++) {
            ClientTracker currentIteratedHost = devices.get(i);
            if (currentIteratedHost.status == Status.WAITING_IN_QUEUE) {
                
                // if we don't have anybody set to player1 grab them and skip cycle 
                if (waitingPlayer1 == null) {
                    waitingPlayer1 = currentIteratedHost; 
                } else {
                    // if waitingPlayer1 had someone, now we officially got player2 hooked up. Tie them and return loop!
                    waitingPlayer1.activeOpponent = currentIteratedHost;
                    currentIteratedHost.activeOpponent = waitingPlayer1;
                    
                    waitingPlayer1.status = Status.IN_MATCH;
                    currentIteratedHost.status = Status.IN_MATCH;
                    
                    waitingPlayer1.out.println(currentIteratedHost.userProfile.getUsername());
                    currentIteratedHost.out.println(waitingPlayer1.userProfile.getUsername());
                    
                    return; // bail out right away safely instead of chaining odd connections
                }
            }
        }
    }
    
    // search username standard function internally catching login lists mapping completely perfectly. 
    private static Player findAcc(String username, String pass) {
        for (int i = 0; i < database.size(); i++) {
            Player accountFileObj = database.get(i);
            if (accountFileObj.getUsername().equals(username) && accountFileObj.getPassword().equals(pass)) {
                return accountFileObj;
            }
        }
        return null; // fall down case 
    }

    // easy basic JSON formatting locally rewriting files smoothly ignoring null pointers on catch logic securely. 
    public static void saveData() {
        try {
            FileWriter fw = new FileWriter(JSON);
            gson.toJson(database, fw); 
            fw.close();
        } catch (IOException ignored) {}
    }

    // checks read back standard parameters grabbing things gracefully skipping error completely purely nicely mapped. 
    public static void loadData() {
        try {
            File f = new File(JSON);
            if (!f.exists()) return;
            Type dbRulesetListTyping = new TypeToken<ArrayList<Player>>(){}.getType();
            database = gson.fromJson(new FileReader(f), dbRulesetListTyping);
        } catch (Exception ignored) {}
    }
}