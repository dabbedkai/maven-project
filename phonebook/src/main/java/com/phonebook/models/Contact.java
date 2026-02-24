package com.phonebook.models;

public class Contact {
    private String name;
    private String phoneNumber;
    private String email;

    public Contact(String name, String phoneNumber, String email) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    public String getName() {
        return name;
    }
 
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setCourse(String email) {
        this.email = email;
    }

    public String toCsvString() {
        return name + "," + phoneNumber + "," + email;
    }

    public String toString() {
        return "Name: " + name + " | Phone: " + phoneNumber + " | Email: " + email;
    }
}
