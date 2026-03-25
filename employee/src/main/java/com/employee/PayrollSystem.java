package com.employee;
import java.util.ArrayList;
import java.util.Scanner;

public class PayrollSystem {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        FileHandler fileHandler = new FileHandler();
        
        ArrayList<Employee> employeeList = new ArrayList<>();

        System.out.println("--- Employee Payroll Persistence System ---");

        while (true) {
            System.out.println("\n[1] Add Employee");
            System.out.println("[2] View All Employees");
            System.out.println("[3] Save Records");
            System.out.println("[4] Load Records");
            System.out.println("[5] Exit");
            System.out.print("Choose option: ");
            
            String choice = scanner.nextLine();

            if (choice.equals("1")) {

                System.out.print("Enter Name: ");
                String name = scanner.nextLine();
                
                System.out.print("Enter Employee ID: ");
                String id = scanner.nextLine();
                
                System.out.println("Choose Employee Type -> (A) Salaried | (B) Hourly: ");
                String typeChoice = scanner.nextLine().toUpperCase();

                Employee newEmployee = null; 

                if (typeChoice.equals("A")) {
                    System.out.print("Enter Base Salary: ");
                    double salary = Double.parseDouble(scanner.nextLine());
                    System.out.print("Enter Expected Bonus: ");
                    double bonus = Double.parseDouble(scanner.nextLine());
                    newEmployee = new SalariedEmployee(name, id, salary, bonus);
                } 
                else if (typeChoice.equals("B")) {
                    System.out.print("Enter Hours Worked: ");
                    int hours = Integer.parseInt(scanner.nextLine());
                    System.out.print("Enter Hourly Rate: ");
                    double rate = Double.parseDouble(scanner.nextLine());
                    newEmployee = new HourlyEmployee(name, id, hours, rate);
                } 
                else {
                    System.out.println("Invalid selection!");
                    continue;
                }

                if (!employeeList.contains(newEmployee)) {
                    employeeList.add(newEmployee);
                    System.out.println("Success: Added " + name + " to the register!");
                } else {
                    System.out.println("Fail: This Employee ID is already registered.");
                }
                
            } else if (choice.equals("2")) {
                if (employeeList.isEmpty()) {
                    System.out.println("Warning: Registry is currently empty!");
                } else {
                    System.out.println("\n=== EMPLOYEE ROSTER ===");
               
                    for (Employee e : employeeList) {
                        System.out.println(e.toString());
                       
                        System.out.println(" >> Calculated Payout: $" + e.calculateEarnings() + "\n");
                    }
                }

            } else if (choice.equals("3")) {
                fileHandler.saveRecords(employeeList);

            } else if (choice.equals("4")) {
                ArrayList<Employee> incomingData = fileHandler.loadRecords();
                if(!incomingData.isEmpty()) {
                    employeeList = incomingData; 
                }

            } else if (choice.equals("5")) {
                System.out.println("Logging off...");
                break;
            } else {
                System.out.println("Input out of range.");
            }
        }
        
        scanner.close();
    }
}
