package com.manabrew.network;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        // hardcoding localhost cause we are testing it on the same PC
        try (
            Socket cSocket = new Socket("localhost", 8080);
            BufferedReader fromTavern = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
            PrintWriter toTavern = new PrintWriter(cSocket.getOutputStream(), true);
            Scanner inReader = new Scanner(System.in)
        ) {
            // this thread just spews server broadcasts straight to our UI
            new Thread(() -> {
                try {
                    String serverTxt;
                    while ((serverTxt = fromTavern.readLine()) != null) {
                        System.out.println(serverTxt);
                    }
                } catch (IOException e) {
                    System.out.println(TerminalColors.RED + "\n[CRITICAL] Server crashed or booted you." + TerminalColors.RESET);
                    System.exit(0);
                }
            }).start();

            // MAmainIN interaction loop down here
            while (true) {
                String pTyped = inReader.nextLine();
                toTavern.println(pTyped);
                
                // graceful quit check
                if (pTyped.equalsIgnoreCase("quit")) {
                    System.out.println("shutting down client terminal...");
                    break;
                }
            }
            
        } catch (Exception err) {
            System.out.println("Connection Failed! Big yikes. Did you launch Server.java first?");
        }
    }
}