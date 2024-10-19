package com.mgke.da.models;

import java.time.LocalDate;
<<<<<<< HEAD
import java.util.Date;
=======
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee

public class PersonalData {

    public String id;
    public String username;
    public String password;
    public String email;
    public String firstName;
    public String lastName;
    public String gender;
<<<<<<< HEAD
    public Date birthDate;
    public String country;
    public String profession;
    public String notes;
    public String avatarUrl;
    public String currency;
=======
    public LocalDate birthDate;
    public String country;
    public String profession;
    public String notes;
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee

    public PersonalData() {
    }

<<<<<<< HEAD
    public PersonalData(String id, String username, String password, String email, String firstname, String lastname, String gender, Date birthDate, String country, String profession, String notes, String avatarUrl, String currency) {
=======
    public PersonalData(String id, String username, String password, String email, String firstname, String lastname, String gender, LocalDate birthDate, String country, String profession, String notes) {
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
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
<<<<<<< HEAD
        this.avatarUrl = avatarUrl;
        this.currency = currency;
    }
    public String getPassword() {
        return password;
    }

=======
    }
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
}