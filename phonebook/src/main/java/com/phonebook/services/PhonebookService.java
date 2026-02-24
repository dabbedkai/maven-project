package com.phonebook.services;

import java.io.*;
import java.util.*;

import com.phonebook.models.Contact;

public class PhonebookService {
    private HashMap<String, Contact> contacts;

    public PhonebookService() {
        this.contacts = new HashMap<>();
    }

    public void addContact(Contact contacts) {
        this.contacts.put(contacts.getName(), contacts);
    }

    public Contact searchContact(String name) {
        return contacts.get(name);
    }

    public boolean removeContact(String name) {
        if(contacts.containsKey(name)){
            contacts.remove(name);
            return true;
        }
        return false;
    }

    public Collection<Contact> getAllContacts() {
        return contacts.values();
    }

    public void loadFromCSV(File filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    Contact contact = new Contact(parts[0], parts[1], parts[2]);
                    addContact(contact);
                }
            }
        }
        }

     public void saveToCSV(String filepath) throws IOException {
        File file = new File(filepath);
        
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            
            writer.write("Name,PhoneNumber,Email");
            writer.newLine();

            for (Contact contact : contacts.values()) {
                writer.write(contact.toCsvString());
                writer.newLine();
            }
        }
    }
}
