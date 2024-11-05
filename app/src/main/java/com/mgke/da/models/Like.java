    package com.mgke.da.models;

    public class Like {
        private String userId;

        public Like() {
            // Пустой конструктор для Firebase
        }

        public Like(String userId) {
            this.userId = userId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }
