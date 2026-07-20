package com.example.demo.service;

import com.example.demo.dto.ReviewRequest;
import com.example.demo.dto.ReviewResponse;
import com.example.demo.entity.Book;
import com.example.demo.entity.Review;
import com.example.demo.exception.BookNotFound;
import com.example.demo.mapper.ReviewMapper;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final ReviewMapper reviewMapper;

    public ReviewResponse addReview(Long Id, ReviewRequest reviewRequest) {
        Book book= bookRepository.findById(Id).orElseThrow(() -> new BookNotFound("No Book Found"));
        Review review=reviewMapper.toEntity(reviewRequest);
        review.setBook(book);
        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    public List<ReviewResponse> getReviews(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFound("No Book Found");
        }
        return reviewRepository.findReviewsByBookId(bookId)
                .stream()
                .map(reviewMapper::toResponse)
                .toList();
    }
}
