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
    // global counters written to the shift log on shutdown
    public static int totalGold        = 0;
    public static int potionsDelivered = 0;

    // all active lobbies keyed by their room code
    private static Map<String, GameRoom> activeRooms = new ConcurrentHashMap<>();

    // returns an existing room or creates a new one; synchronized so two clients
    // hitting the same code at the same time don't race-create two rooms
    public static synchronized GameRoom getOrCreateRoom(String code) {
        if (!activeRooms.containsKey(code)) {
            activeRooms.put(code, new GameRoom(code));
            System.out.println(TerminalColors.YELLOW + "new lobby: [" + code + "]" + TerminalColors.RESET);
        }
        return activeRooms.get(code);
    }

    public static void main(String[] args) {
        // registers a shutdown hook so stats are always saved on ctrl+c or normal exit
        setupSaveHook();

        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println(TerminalColors.CYAN + "ManaBrew server is online on port 8080." + TerminalColors.RESET);
            System.out.println("waiting for players...");

            while (true) {
                // each connecting client gets its own thread; room assignment happens later in ClientHandler
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println(TerminalColors.RED + "port 8080 is already in use. kill whatever's on it and retry." + TerminalColors.RESET);
        }
    }

    // writes shift analytics to shift_logs.json when the process exits
    private static void setupSaveHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Gson gson      = new GsonBuilder().setPrettyPrinting().create();
            File logFile   = new File("shift_logs.json");
            ArrayList<ShiftLog> history = new ArrayList<>();

            // load existing history if the file is already there
            if (logFile.exists()) {
                try (Reader r = new FileReader(logFile)) {
                    ShiftLog[] old = gson.fromJson(r, ShiftLog[].class);
                    if (old != null) history.addAll(Arrays.asList(old));
                } catch (Exception e) {
                    System.out.println("existing shift_logs.json was unreadable, starting fresh.");
                }
            }

            // append this session's numbers
            history.add(new ShiftLog(LocalDateTime.now().toString(), potionsDelivered, totalGold));

            // dump back to disk
            try (Writer w = new FileWriter(logFile)) {
                gson.toJson(history, w);
                System.out.println(TerminalColors.GREEN + "[saved] shift analytics written to shift_logs.json" + TerminalColors.RESET);
            } catch (IOException e) {
                System.out.println(TerminalColors.RED + "[error] could not write shift_logs.json" + TerminalColors.RESET);
            }
        }));
    }
}