package com.phonebook;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.phonebook.models.Contact;
import com.phonebook.services.PhonebookService;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);

        PhonebookService service = new PhonebookService();
        boolean running = true;


        while (running) {
            service.loadFromCSV(new File("phonebook/data/contacts.csv"));
            
            System.out.println("""

                      --- Menu ---
                    [1] Add contact
                    [2] Search contact
                    [3] Remove contact
                    [4] Display all contact
                    [5] Save to CSV
                    [0] Exit

                    Enter the service you want to choose:
                        """);
            String inputChoice = sc.nextLine();

            switch (inputChoice) {
                case "1" -> {
                    System.out.print("Enter Name: ");
                    String name = sc.nextLine();
                    System.out.print("Enter Phone Number: ");
                    String phone = sc.nextLine();
                    System.out.print("Enter Email: ");
                    String email = sc.nextLine();

                    Contact newContact = new Contact(name, phone, email);
                    service.addContact(newContact);
                    System.out.println("\nContact added successfully.");
                }
                case "2" -> {
                    System.out.print("Enter Name to search: ");
                    String name = sc.nextLine();
                    Contact found = service.searchContact(name);

                    if(found != null){
                        System.out.println("\nContact search successfully");
                    }else{
                        System.out.println("\nContact not found");
                    }
                }
                case "3" -> {
                    System.out.print("Enter name to remove: ");
                    String removeName = sc.nextLine();

                    boolean isRemoved = service.removeContact(removeName);

                    if(isRemoved){
                        System.out.println("\nContact is removed");
                    }else{
                        System.out.println("\nContact not found, nothing removed");
                    }
                }
                case "4" -> {
                    System.out.println("\n--- All Contacts ---");
                    Collection<Contact> allContacts = service.getAllContacts();
                    if (allContacts.isEmpty()) {
                        System.out.println("(Phonebook is empty)");
                    } else {
                        for (Contact c : allContacts) {
                            System.out.println(c);
                        }
                    }
                }
                case "5" -> {
                    System.out.println("Saving to 'contacts.csv'");
                    try {
                        service.saveToCSV("phonebook/data/contacts.csv");
                        System.out.println("Data successfully saved to contacts.csv");
                    } catch (IOException e) {
                        System.out.println("Error saving file: " + e.getMessage());
                    }
                }
                case "0" -> {
                    running = false;
                    System.out.println("Exiting application");
                }

                default -> {
                    System.out.println("Invalid choice, try again");
                }
            }
        }
    }
}