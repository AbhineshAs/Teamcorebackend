package com.example.crm.repository;

import com.example.crm.entity.CallRecord;
import com.example.crm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CallRecordRepository extends JpaRepository<CallRecord, Long> {
    List<CallRecord> findByAgent(User agent);
}
