package com.manabrew.network;

import com.manabrew.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    // global stat tracking for the final JSON payload
    public static int totalGold = 0;
    public static int potionsDelivered = 0;

    // hold all distinct lobbies
    private static Map<String, GameRoom> activeRooms = new ConcurrentHashMap<>();

    // generation or retrieval of rooms based on lobby code
    public static synchronized GameRoom getOrCreateRoom(String code) {
        if (!activeRooms.containsKey(code)) {
            activeRooms.put(code, new GameRoom(code));
            System.out.println(TerminalColors.YELLOW + "DEBUG: Booted up new Lobby Node: [" + code + "]" + TerminalColors.RESET);
        }
        return activeRooms.get(code);
    }

    public static void main(String[] args) {
        // i/o and persistence setup 
        setupSaveHook(); 

        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println(TerminalColors.CYAN + "ManaBrew Central Server is Online!" + TerminalColors.RESET);
            System.out.println("Waiting for alchemists on Port 8080...");
            System.out.println("Note: Global active round loops are now distributed across Room Nodes.");

            while (true) {
                // blindly accept sockets, room routing happens after they chat with ClientHandler
                Socket clientSocket = serverSocket.accept();
                ClientHandler pHandler = new ClientHandler(clientSocket);
                new Thread(pHandler).start(); 
            }
        } catch (IOException e) {
            System.out.println(TerminalColors.RED + "Network fail: couldn't map to 8080. Something is already running there." + TerminalColors.RESET);
        }
    }

    // handles writing stats perfectly on server closure
    private static void setupSaveHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // pretty-printing for prettier text
            Gson gsonEngine = new GsonBuilder().setPrettyPrinting().create();
            File logStorage = new File("shift_logs.json");
            
            ArrayList<ShiftLog> pastShifts = new ArrayList<>();

            // load history if the json drive already exists
            if (logStorage.exists()) {
                try (Reader fileReader = new FileReader(logStorage)) {
                    ShiftLog[] loadedArray = gsonEngine.fromJson(fileReader, ShiftLog[].class);
                    if (loadedArray != null) {
                        pastShifts.addAll(Arrays.asList(loadedArray));
                    }
                } catch (Exception err) {
                    System.out.println("debug: old json file looks busted, creating a fresh one.");
                }
            }

            // push this shift's local totals onto the list
            pastShifts.add(new ShiftLog(
                LocalDateTime.now().toString(), 
                potionsDelivered, 
                totalGold
            ));

            //dump the updated tracker straight back into the json
            try (Writer fileWriter = new FileWriter(logStorage)) {
                gsonEngine.toJson(pastShifts, fileWriter);
                System.out.println(TerminalColors.GREEN + "\n[SYSTEM] Safely flushed shift analytics to shift_logs.json!" + TerminalColors.RESET);
            } catch (IOException err) {
                System.out.println(TerminalColors.RED + "\n[SYSTEM] Write operation failed!" + TerminalColors.RESET);
            }
        }));
    }
}