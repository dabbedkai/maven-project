package com.employee;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class FileHandler {
    private static final String FILE_NAME = "employees.json";
    private Gson gson;

    public FileHandler() {

        RuntimeTypeAdapterFactory<Employee> adapter = RuntimeTypeAdapterFactory
            .of(Employee.class, "type") 
            .registerSubtype(SalariedEmployee.class, EmployeeType.SALARIED.name())
            .registerSubtype(HourlyEmployee.class, EmployeeType.HOURLY.name());

        gson = new GsonBuilder()
                .registerTypeAdapterFactory(adapter)
                .setPrettyPrinting()
                .create();
    }

    // Save Logic
    public void saveRecords(ArrayList<Employee> employees) {
        try (Writer writer = new FileWriter(FILE_NAME)) {
            gson.toJson(employees, writer);
            System.out.println("Data saved successfully to " + FILE_NAME);
        } catch (IOException e) {
            System.out.println("Could not save to file.");
        }
    }

    public ArrayList<Employee> loadRecords() {

        try (Reader reader = new FileReader(FILE_NAME)) {

            Type listType = new TypeToken<ArrayList<Employee>>(){}.getType();
            ArrayList<Employee> loadedEmployees = gson.fromJson(reader, listType);
            
            if (loadedEmployees != null) {

                System.out.println("Records successfully loaded from " + FILE_NAME);
                return loadedEmployees;

            }
        } catch (FileNotFoundException e) {

            System.out.println("No prior file found. System starting fresh.");

        } catch (IOException e) {

            System.out.println("File exists but failed to read.");

        }

        return new ArrayList<>(); 
    }
}
