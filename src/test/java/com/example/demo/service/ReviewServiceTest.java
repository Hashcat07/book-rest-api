package com.example.demo.service;

import com.example.demo.dto.ReviewRequest;
import com.example.demo.dto.ReviewResponse;
import com.example.demo.entity.Book;
import com.example.demo.entity.Review;
import com.example.demo.exception.BookNotFound;
import com.example.demo.mapper.ReviewMapper;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void testAddReview() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Java");

        ReviewRequest request = new ReviewRequest(5, "Great");
        Review review = new Review();
        review.setRating(5);
        review.setComment("Great");
        ReviewResponse response = new ReviewResponse(1L, 5, "Great");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(reviewMapper.toEntity(request)).thenReturn(review);
        when(reviewRepository.save(review)).thenReturn(review);
        when(reviewMapper.toResponse(review)).thenReturn(response);

        ReviewResponse result = reviewService.addReview(1L, request);

        assertEquals(5, result.getRating());
        assertEquals("Great", result.getComment());
        assertEquals(book, review.getBook()); // review was linked to the book
        verify(reviewRepository).save(review);
    }

    @Test
    void testAddReviewBookNotFound() {
        ReviewRequest request = new ReviewRequest(5, "Great");

        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BookNotFound.class, () -> reviewService.addReview(99L, request));
        verify(reviewRepository, never()).save(any());
    }
}
