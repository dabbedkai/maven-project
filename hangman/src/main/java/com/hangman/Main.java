package com.hangman;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
   private static final String JSON_FILE = "users.json";
    private static List<User> users = new ArrayList<>();
    private static User loggedInUser = null;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
 
        loadUsers();

        while (true) {
            System.out.println("\n=== HANGMAN LOGIN SYSTEM ===");
            System.out.println("1. Login");
            System.out.println("2. Register new account");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            
            String choice = sc.nextLine();

            if (choice.equals("1")) {
                if (login(sc)) {
                    mainMenu(sc);
                }
            } else if (choice.equals("2")) {
                register(sc);
            } else if (choice.equals("3")) {
                System.out.println("Goodbye!");
                break;
            } else {
                System.out.println("Invalid choice. Try again.");
            }
        }
        sc.close();
    }

    public static boolean login(Scanner sc) {
        System.out.print("Enter username: ");
        String username = sc.nextLine();
        System.out.print("Enter password: ");
        String password = sc.nextLine();

        for (User u : users) {
            if (u.username.equalsIgnoreCase(username) && u.password.equals(password)) {
                loggedInUser = u;
                System.out.println("\nLogin successful! Welcome back, " + u.username + "!");
                return true;
            }
        }
        System.out.println("\nInvalid username or password.");
        return false;
    }

    public static void register(Scanner sc) {
        System.out.print("Enter new username: ");
        String username = sc.nextLine();

        for (User u : users) {
            if (u.username.equalsIgnoreCase(username)) {
                System.out.println("\nUsername already exists! Try logging in.");
                return;
            }
        }

        System.out.print("Enter new password: ");
        String password = sc.nextLine();

        User newUser = new User(username, password, 0);
        users.add(newUser);
        saveUsers();
        System.out.println("\nAccount created successfully! You can now log in.");
    }


    public static void mainMenu(Scanner sc) {
        while (loggedInUser != null) {
            System.out.println("\n=== MAIN MENU ===");
            System.out.println("Player: " + loggedInUser.username + " | Score: " + loggedInUser.score);
            System.out.println("1. Play Hangman");
            System.out.println("2. Leaderboard");
            System.out.println("3. Logout");
            System.out.print("Choose an option: ");
            
            String choice = sc.nextLine();

            if (choice.equals("1")) {
                playGame(sc);
            } else if (choice.equals("2")) {
                displayLeaderboard();
            } else if (choice.equals("3")) {
                loggedInUser = null;
                System.out.println("Logged out successfully.");
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }

    public static void playGame(Scanner sc) {
        System.out.println("\nSelect Difficulty:");
        System.out.println("1. Easy\n2. Medium\n3. Hard");
        System.out.print("Choice: ");
        String diff = sc.nextLine();

        String filename = "data/easy.txt";
        int scoreReward = 10;

        if (diff.equals("2")) {
            filename = "data/medium.txt";
            scoreReward = 20;
        } else if (diff.equals("3")) {
            filename = "hard.txt";
            scoreReward = 30;
        }

        List<String> words = readWordsFromFile(filename);
        if (words.isEmpty()) {
            System.out.println("Error: No words found in " + filename);
            return;
        }

        String wordToGuess = words.get((int) (Math.random() * words.size())).toLowerCase();
        int maxAttempts = 6;
        int correctGuesses = 0;
        char[] blankSpace = new char[wordToGuess.length()];
        Arrays.fill(blankSpace, '_');

        System.out.println("\nGame started! Difficulty: " + filename.replace(".txt", "").toUpperCase());

        while (true) {
            System.out.println("\nAttempts left: " + maxAttempts);
            for (char c : blankSpace) {
                System.out.print(c + " ");
            }
            System.out.print("\nChoose a letter to guess the word: ");
            
            String input = sc.nextLine().toLowerCase();
            if (input.isEmpty()) continue;
            char guess = input.charAt(0);

            boolean found = false;
            boolean alreadyGuessed = true;

            for (int i = 0; i < wordToGuess.length(); i++) {
                if (wordToGuess.charAt(i) == guess) {
                    found = true;
                    if (blankSpace[i] == '_') {
                        blankSpace[i] = guess;
                        correctGuesses++;
                        alreadyGuessed = false;
                    }
                }
            }

            if (found) {
                if (alreadyGuessed) {
                    System.out.println("You already guessed '" + guess + "'.");
                } else {
                    System.out.println(guess + " is a great guess!");
                }
            } else {
                System.out.println(guess + " is not in the word.");
                maxAttempts--;
            }

            if (correctGuesses == wordToGuess.length()) {
                System.out.println("\nCongratulations! You've guessed the word: " + wordToGuess);
                System.out.println("You earned " + scoreReward + " points!");
                loggedInUser.score += scoreReward;
                saveUsers(); // Save updated score to JSON
                break;
            }

            if (maxAttempts == 0) {
                System.out.println("\nGame Over! The word was: " + wordToGuess);
                break;
            }
        }
    }

    public static void displayLeaderboard() {
        System.out.println("\n===================================");
        System.out.println("           LEADERBOARD             ");
        System.out.println("===================================");
        System.out.printf("%-15s | %-5s%n", "NAME", "SCORE");
        System.out.println("-----------------------------------");

        // Sort users by score (descending)
        users.sort((u1, u2) -> Integer.compare(u2.score, u1.score));

        for (User u : users) {
            System.out.printf("%-15s | %d%n", u.username, u.score);
        }
        System.out.println("===================================");
    }


    public static List<String> readWordsFromFile(String filename) {
        try {
            return Files.readAllLines(Paths.get(filename));
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static void saveUsers() {
        try {
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < users.size(); i++) {
                sb.append("  ").append(users.get(i).toJson());
                if (i < users.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]");
            Files.write(Paths.get(JSON_FILE), sb.toString().getBytes());
        } catch (IOException e) {
            System.out.println("Error saving user data.");
        }
    }

    public static void loadUsers() {
        users.clear();
        try {
            String content = new String(Files.readAllBytes(Paths.get(JSON_FILE)));

            Pattern p = Pattern.compile("\\{\"username\":\"(.*?)\", \"password\":\"(.*?)\", \"score\":(\\d+)\\}");
            Matcher m = p.matcher(content);
            
            while (m.find()) {
                users.add(new User(m.group(1), m.group(2), Integer.parseInt(m.group(3))));
            }
        } catch (IOException e) {
            System.out.println("Error loading user data.");
        }
    }
}