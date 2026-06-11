package com.techbleat.bank.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class BankTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "transaction_type")
    private String transactionType;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "reference")
    private String reference;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getTransactionType() { return transactionType; }
    public Double getAmount() { return amount; }
    public String getReference() { return reference; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public void setAmount(Double amount) { this.amount = amount; }
    public void setReference(String reference) { this.reference = reference; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
