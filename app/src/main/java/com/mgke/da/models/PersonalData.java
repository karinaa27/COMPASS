package com.mgke.da.models;

import java.time.LocalDate;

public class PersonalData {

    public String id;
    public String username;
    public String password;
    public String email;
    public String firstName;
    public String lastName;
    public String gender;
    public LocalDate birthDate;
    public String country;
    public String profession;
    public String notes;

    public PersonalData() {
    }

    public PersonalData(String id, String username, String password, String email, String firstname, String lastname, String gender, LocalDate birthDate, String country, String profession, String notes) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.firstName = firstname;
        this.lastName = lastname;
        this.gender = gender;
        this.birthDate = birthDate;
        this.country = country;
        this.profession = profession;
        this.notes = notes;
    }
}