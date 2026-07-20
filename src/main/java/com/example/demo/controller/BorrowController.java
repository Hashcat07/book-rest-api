package com.example.demo.controller;

import com.example.demo.dto.BookResponse;
import com.example.demo.dto.BorrowRequest;
import com.example.demo.dto.BorrowResponse;
import com.example.demo.service.BorrowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    @PostMapping("/borrow")
    public ResponseEntity<String> borrow(@RequestBody @Valid BorrowRequest borrowRequest,
                                         Authentication authentication) {
        return ResponseEntity.ok(borrowService.borrowBook(authentication.getName(), borrowRequest.getBookId()));
    }

    @PostMapping(value = "/return/{bookId}",produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
    })
    public ResponseEntity<BookResponse> returnBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(borrowService.returnBook(bookId));
    }

    @GetMapping("/borrow/me")
    public ResponseEntity<List<BorrowResponse>> myHistory(Authentication authentication) {
        return ResponseEntity.ok(borrowService.getMyHistory(authentication.getName()));
    }
}
