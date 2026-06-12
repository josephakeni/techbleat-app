package com.techbleat.bank.repo;

import com.techbleat.bank.model.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
    List<BankTransaction> findByUserIdOrderByCreatedAtDesc(String userId);
}
