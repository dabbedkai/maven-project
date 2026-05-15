package com.manabrew.network;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        // hardcoded localhost since we're running both sides on the same machine
        try (
            Socket cSocket      = new Socket("localhost", 8080);
            BufferedReader fromServer = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
            PrintWriter    toServer   = new PrintWriter(cSocket.getOutputStream(), true);
            Scanner        keyboard   = new Scanner(System.in)
        ) {
            // background thread: dumps everything the server sends straight to the terminal
            // the server handles all screen clearing and drawing, so we just print blindly
            Thread listener = new Thread(() -> {
                try {
                    String line;
                    while ((line = fromServer.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    System.out.println(TerminalColors.RED + "\n[disconnected] server closed the connection." + TerminalColors.RESET);
                    System.exit(0);
                }
            });
            listener.setDaemon(true);
            listener.start();

            // main input loop: read what the player types, ship it to the server
            while (true) {
                String input = keyboard.nextLine();
                toServer.println(input);

                if (input.equalsIgnoreCase("quit")) {
                    System.out.println("closing client...");
                    break;
                }
            }

        } catch (Exception e) {
            System.out.println("couldn't connect. is Server.java running on port 8080?");
        }
    }
}