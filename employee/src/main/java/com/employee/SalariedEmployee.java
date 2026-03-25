package com.employee;

public class SalariedEmployee extends Employee {
    private double baseSalary;
    private double bonus;

    public SalariedEmployee(String name, String employeeId, double baseSalary, double bonus) {

        super(name, employeeId, EmployeeType.SALARIED);
        this.baseSalary = baseSalary;
        this.bonus = bonus;
    }

    @Override
    public double calculateEarnings() {
        return baseSalary + bonus;
    }
}
