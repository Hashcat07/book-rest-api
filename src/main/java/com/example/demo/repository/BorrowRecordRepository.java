package com.example.demo.repository;

import com.example.demo.entity.BorrowRecord;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord,Long> {
    Optional<BorrowRecord> findByIdAndReturnDateIsNull(Long id);
    List<BorrowRecord> findByUserId(Long userId);
}
