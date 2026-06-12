package com.techbleat.bank.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "balance")
    private Double balance;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Double getBalance() {
        return balance == null ? 0.0 : balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }
}
