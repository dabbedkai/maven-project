package com.roshambo;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 6767;

        try (Socket socket = new Socket(serverAddress, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner sc = new Scanner(System.in)) {

            System.out.println("Connecting to the game Server...");
            String serverMessage;

            while ((serverMessage = in.readLine()) != null) {
                // if the server forces client closure logic securely terminates process looping properly 
                if (serverMessage.equalsIgnoreCase("QUIT_CLIENT") || serverMessage.equalsIgnoreCase("Game ended. Disconnecting...")) {
                    System.out.println("Disconnected from server.");
                    break;
                }

                // handling basic menu prompts mapping terminal blocks
                if (serverMessage.equalsIgnoreCase("INPUT_REQUIRED")) {
                    System.out.print("> ");
                    out.println(sc.nextLine());
                } 
                // handling specific in-game moves
                else if (serverMessage.equalsIgnoreCase("YOUR_TURN_RPS")) {
                    System.out.print("Enter your move (0 = rock, 1 = paper, 2 = scissors) or type 'quit': ");
                    out.println(sc.nextLine());
                } 
                else {
                    System.out.println("[SERVER] " + serverMessage);
                }
            }

        } catch (IOException e) {
            System.out.println("Connection error: Server is offline or unavailable.");
        }
    }
}