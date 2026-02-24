package com.dabbedkai;
import java.util.*;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        ArrayList<Student> students = new ArrayList<>();

        

        try (Scanner sc = new Scanner(new File("data/students.csv"))) {
            while(sc.hasNextLine()){
                String line = sc.nextLine();
                String[] fields = line.split(",");

                 if (fields.length >= 3) {
                        String name = fields[0];
                        String age = fields[1];
                        String course = fields[2];

                        Student student = new Student(name, age, course);
                        students.add(student);

                        System.out.printf("""
                            =========================
                            Name: %s
                            Age: %s
                            Course: %s
                            """
                            , name, age, course);
                    }
            }
            System.out.println("=========================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}