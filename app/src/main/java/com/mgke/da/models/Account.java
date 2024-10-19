package com.mgke.da.models;

<<<<<<< HEAD
public class Account {
=======
public class Account{
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee

    public String id;
    public double accountAmount;
    public String accountName;
<<<<<<< HEAD
    public String userId;
    public String currency;
    public String background;
=======
    public String username;
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee

    public Account() {
    }

<<<<<<< HEAD
    // Обновленный конструктор
    public Account(String id, double accountAmount, String accountName, String userId, String currency, String background) {
        this.id = id;
        this.accountAmount = accountAmount;
        this.accountName = accountName;
        this.userId = userId;
        this.currency = currency;
        this.background = background;
    }

}
=======
    public Account(String id, double accountAmount, String accountName, String username) {
        this.id = id;
        this.accountAmount = accountAmount;
        this.accountName = accountName;
        this.username = username;
    }
}
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
