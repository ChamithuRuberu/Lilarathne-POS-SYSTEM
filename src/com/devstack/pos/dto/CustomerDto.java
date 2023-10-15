package com.devstack.pos.dto;

public class CustomerDto {
    private String email;
    private String name;
    private String contact;
    private double salary;

    public CustomerDto() {
    }

    public CustomerDto(String email, String name, String contact, double salary) {
        this.email = email;
        this.name = name;
        this.contact = contact;
        this.salary = salary;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }
}
