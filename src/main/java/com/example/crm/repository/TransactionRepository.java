package com.example.crm.repository;

import com.example.crm.entity.Transaction;
import com.example.crm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // Used for generating P&L and VAT reports for specific periods
    List<Transaction> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);
    
    List<Transaction> findByType(String type);

    List<Transaction> findByRecordedBy(User recordedBy);
}