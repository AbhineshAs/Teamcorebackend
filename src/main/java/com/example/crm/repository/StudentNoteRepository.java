package com.example.crm.repository;

import com.example.crm.entity.StudentNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentNoteRepository extends JpaRepository<StudentNote, Long> {
    List<StudentNote> findByStudentId(Long studentId);
}
