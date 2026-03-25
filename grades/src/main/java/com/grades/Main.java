package com.grades;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        ArrayList<Grades> gradeList = new ArrayList<>();
        
        String[] defaultSubjects = {"COMPRO1", "COMPRO2", "OOP", "DSA", "MMW"};
        String[] gradeSemester = {"PRELIMS", "MIDTERMS", "FINALS"};

        try (Scanner sc = new Scanner(System.in)) {

            for (String subject : defaultSubjects) {
                System.out.println("Enter grade for " + subject + ": ");
                
                int[] tempGrades = new int[3];
                for (int j = 0; j < 3; j++) {
                    System.out.print(gradeSemester[j] + ": ");
                    tempGrades[j] = sc.nextInt();
                    sc.nextLine(); 
                }
                
                gradeList.add(new Grades(subject, tempGrades[0], tempGrades[1], tempGrades[2]));
            }

            String strAnotherP;
            char cAnotherP;

            do { 
                System.out.print("\nADD ANOTHER GRADE Y/N? ");
                strAnotherP = sc.nextLine();
                
                cAnotherP = strAnotherP.length() > 0 ? strAnotherP.charAt(0) : 'N';
                
                if (cAnotherP == 'Y' || cAnotherP == 'y') {
                    System.out.print("Enter subject name: ");
                    String newSubject = sc.nextLine();
                    
                    System.out.println("Enter grade for " + newSubject + ": ");
                    int[] tempGrades = new int[3];
                    
                    for (int j = 0; j < 3; j++) {
                        System.out.print(gradeSemester[j] + ": ");
                        tempGrades[j] = sc.nextInt();
                        sc.nextLine();
                    }

                    gradeList.add(new Grades(newSubject, tempGrades[0], tempGrades[1], tempGrades[2]));
                }
            } while (cAnotherP == 'Y' || cAnotherP == 'y');

        } 

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[\n");
        
        for (int i = 0; i < gradeList.size(); i++) {
            jsonBuilder.append(gradeList.get(i).toJson());
            
            if (i < gradeList.size() - 1) {
                jsonBuilder.append(",\n");
            } else {
                jsonBuilder.append("\n");
            }
        }
        jsonBuilder.append("]\n");

        try (FileWriter fw = new FileWriter("data.json")) {
            fw.write(jsonBuilder.toString());
            System.out.println("\nSuccessfully saved to data.json!");
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }
}