package com.example.demo.controller;

import com.example.demo.dto.ReviewRequest;
import com.example.demo.dto.ReviewResponse;
import com.example.demo.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/books/{bookId}/reviews")
    public ResponseEntity<ReviewResponse> addReview(@PathVariable Long bookId, @RequestBody @Valid ReviewRequest reviewRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.addReview(bookId, reviewRequest));
    }
}
