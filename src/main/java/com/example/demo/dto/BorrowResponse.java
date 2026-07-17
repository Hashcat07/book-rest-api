package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BorrowResponse {
    private Long id;
    private String userName;
    private String bookTitle;
    private LocalDateTime borrowDate;
    private LocalDateTime returnDate;
}
