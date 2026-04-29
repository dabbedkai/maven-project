package com.manabrew.network;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try (
            // connects to the server port we set in Server.java
            Socket socket = new Socket("localhost", 8080);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in)
        ) {
            // this background thread constantly listens for server broadcasts (like new orders or player updates)
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        System.out.println(msg);
                    }
                } catch (IOException e) {
                    System.out.println(TerminalColors.RED + "\n[ ! ] Disconnected from Tavern Server." + TerminalColors.RESET);
                    System.exit(0);
                }
            }).start();

            // the main thread handles player typing
            while (true) {
                String cmd = scanner.nextLine();
                out.println(cmd);
                
                if (cmd.equalsIgnoreCase("quit")) {
                    break;
                }
            }
            
        } catch (Exception e) {
            System.out.println("Could not connect to the ManaBrew Tavern Server. Make sure you run the Server first!");
        }
    }
}