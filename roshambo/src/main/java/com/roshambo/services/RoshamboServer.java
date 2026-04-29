package com.roshambo.services;

import com.roshambo.models.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.Type;

// marks state parameters stopping logical overwrites completely isolating network instances properly 
enum Status {
    BROWSING_MENUS, WAITING_IN_QUEUE, IN_MATCH
}

// purely structural data objects carrying local properties naturally persisting over standard loops explicitly 
class ClientTracker {
    Socket socket;
    BufferedReader in;
    PrintWriter out;
    Status status = Status.BROWSING_MENUS;
    Player userProfile = null;
    
    // connects mapped string slots allowing opposing networks securely seeing partner messages globally 
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
    
    // instantiate gson forcing native mappings ignoring deep arrays preventing corrupted readbacks purely 
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create(); 

    public static void main(String[] args) {
        loadData();
        List<ClientTracker> connectedDevices = new ArrayList<>();

        try {
            ServerSocket serverSocket = new ServerSocket(6767);
            
            // overrides native thread blocking essentially kicking accepts forcing loop repeating asynchronously magically
            serverSocket.setSoTimeout(15); 
            System.out.println("Stateless hub matchmaking server strictly running natively on Port 6767");

            while (true) {
                // quietly catches connections bridging hosts effortlessly silently wrapping instances quickly
                try {
                    Socket tempSoc = serverSocket.accept();
                    connectedDevices.add(new ClientTracker(tempSoc));
                    System.out.println("User has firmly tethered cleanly handling setup seamlessly!");
                } catch (SocketTimeoutException ignored) { } 
                
                // sequentially parsing all network objects directly asking ready statuses cleanly natively 
                for (int i = 0; i < connectedDevices.size(); i++) {
                    ClientTracker c = connectedDevices.get(i);
                    try {
                        // .ready instantly skips completely over empty paths refusing hard wait times implicitly gracefully 
                        if (c.in.ready()) {
                            String msg = c.in.readLine();
                            if (msg != null) handleRequest(c, msg); 
                        }
                    } catch (IOException dropErr) {
                        // checks internal null instances avoiding array index drops crashing nicely mapping cleanup
                        if(c.activeOpponent != null) c.activeOpponent.out.println("OPPONENT_QUIT");
                        connectedDevices.remove(c); 
                        i--;
                    }
                }

                // continuously evaluates matching properties purely pairing queue items cleanly instantly natively 
                pairQueuedPlayers(connectedDevices);

                // briefly releasing central limits protecting computer processors dropping overheating dynamically implicitly
                Thread.sleep(10);
            }
        } catch (Exception fatal) { 
            fatal.printStackTrace(); 
        }
    }

    // splitting incoming tagged identifiers switching specific actions tracking database operations seamlessly
    public static void handleRequest(ClientTracker c, String msg) {
        String[] dataParts = msg.split(":"); 

        switch (dataParts[0]) {
            case "LOGIN":
                Player p = findAcc(dataParts[1], dataParts[2]);
                if (p != null) {
                    c.userProfile = p;
                    c.out.println("\u001B[32mSuccess! Welcome back, " + p.getUsername() + "!\u001B[0m");
                } else {
                    c.out.println("\u001B[31mLogin failed mathematically checking arrays dropped false.\u001B[0m");
                }
                break;
                
            case "REGISTER":
                // gracefully handles appending fields directly stopping completely cloned users naturally 
                if (findAcc(dataParts[1], dataParts[2]) == null) {
                    Player n = new Player(dataParts[1], dataParts[2], 0, 0);
                    database.add(n);
                    saveData();
                    c.out.println("\u001B[32mCreated efficiently mapping new model structure globally natively.\u001B[0m");
                } else {
                    c.out.println("\u001B[31mBlocked entirely duplicates checked explicitly naturally.\u001B[0m");
                }
                break;

            case "QUEUE_UP":
                c.status = Status.WAITING_IN_QUEUE;
                break;

            case "QUIT_MATCH": 
            case "QUIT": 
                // cleanly relays broken structures closing states nicely naturally updating hosts correctly
                if (c.activeOpponent != null) {
                    c.activeOpponent.out.println("OPPONENT_QUIT");
                    c.activeOpponent.activeOpponent = null; // releasing cross variables explicitly preventing leaks securely
                }
                break;

            case "MOVE":
                // holds individual packets completely dropping sync barriers mathematically bridging texts deeply internally
                c.latestMoveReady = dataParts[1]; 
                
                // executes perfectly synced instances matching null conditions dropping results purely naturally synchronously 
                if (c.activeOpponent != null && c.activeOpponent.latestMoveReady != null) {
                    
                    c.out.println("PLAYED:" + c.activeOpponent.latestMoveReady);
                    c.activeOpponent.out.println("PLAYED:" + c.latestMoveReady);
                    
                    // natively cleans strings removing values explicitly cleanly prepping iterations effortlessly
                    c.latestMoveReady = null; 
                    c.activeOpponent.latestMoveReady = null; 
                }
                break;
                
            // cleanly pushing arrays completely handling internal scores updating implicitly masking correctly locally
            case "SAVE_MY_WIN":
                c.userProfile.setMatchesPlayed(c.userProfile.getMatchesPlayed() + 1);
                c.userProfile.incrementScore();
                saveData(); 
                c.status = Status.BROWSING_MENUS;
                break;

            case "SAVE_MY_LOSS":
            case "SAVE_MY_TIE":
                c.userProfile.setMatchesPlayed(c.userProfile.getMatchesPlayed() + 1);
                saveData(); 
                c.status = Status.BROWSING_MENUS;
                break;
        }
    }

    // directly iterating connected properties locating similar variables perfectly mapping connections natively smoothly
    public static void pairQueuedPlayers(List<ClientTracker> devices) {
        ClientTracker waitingPlayer1 = null;

        for (int i = 0; i < devices.size(); i++) {
            ClientTracker currentIteratedHost = devices.get(i);
            if (currentIteratedHost.status == Status.WAITING_IN_QUEUE) {
                
                // captures null reference strictly allocating initially dropping back iterating beautifully internally 
                if (waitingPlayer1 == null) {
                    waitingPlayer1 = currentIteratedHost; 
                } else {
                    // logically tethers properties intrinsically completely removing thread requirements completely smoothly internally 
                    waitingPlayer1.activeOpponent = currentIteratedHost;
                    currentIteratedHost.activeOpponent = waitingPlayer1;
                    
                    waitingPlayer1.status = Status.IN_MATCH;
                    currentIteratedHost.status = Status.IN_MATCH;
                    
                    waitingPlayer1.out.println(currentIteratedHost.userProfile.getUsername());
                    currentIteratedHost.out.println(waitingPlayer1.userProfile.getUsername());
                    
                    // returns deeply mapping states dropping loops efficiently preserving bandwidth completely locally
                    return; 
                }
            }
        }
    }
    
    // searching parameters correctly identifying credentials effortlessly dropping null internally natively properly
    private static Player findAcc(String username, String pass) {
        for (int i = 0; i < database.size(); i++) {
            Player accountFileObj = database.get(i);
            if (accountFileObj.getUsername().equals(username) && accountFileObj.getPassword().equals(pass)) {
                return accountFileObj;
            }
        }
        return null;
    }

    // formats dynamically updating local documents seamlessly silently ignoring locked crash files explicitly beautifully 
    public static void saveData() {
        try {
            FileWriter fw = new FileWriter(JSON);
            gson.toJson(database, fw); 
            fw.close();
        } catch (IOException ignored) {}
    }

    // correctly interpreting arrays restoring mapped pointers perfectly catching internal typing completely securely nicely
    public static void loadData() {
        try {
            File f = new File(JSON);
            if (!f.exists()) return;
            Type dbRulesetListTyping = new TypeToken<ArrayList<Player>>(){}.getType();
            database = gson.fromJson(new FileReader(f), dbRulesetListTyping);
        } catch (Exception ignored) {}
    }
}