    package com.mgke.da.models;

    import java.io.Serializable;
    import java.util.Date;

    public class Transaction implements Serializable {
        private static final long serialVersionUID = 1L;
        public String id;
        public String type;
        public String category;
        public String account;
        public Date date;
        public double amount;
        public String userId;
        public String currency;
        public String nameGoal;
        public int categoryImage;
        public int categoryColor;
        public String accountBackground;

        public Transaction() {
        }

        public Transaction(String id, String type, String category, String account, Date date, double amount, String userId, String currency, String nameGoal, String accountBackground) {
            this.id = id;
            this.type = type;
            this.category = category;
            this.account = account;
            this.date = date;
            this.amount = amount;
            this.userId = userId;
            this.currency = currency;
            this.nameGoal = nameGoal;
            this.accountBackground = accountBackground;
        }

        public double getAmount() {
            return amount;
        }

        public String getType() {
            return type;
        }
    }