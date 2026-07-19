package com.example.demo.service;

import com.example.demo.dto.BookRequest;
import com.example.demo.dto.BookResponse;
import com.example.demo.entity.Book;
import com.example.demo.entity.Category;
import com.example.demo.exception.BookNotFound;
import com.example.demo.exception.CategoryNotFoundException;
import com.example.demo.exception.DuplicateBookException;
import com.example.demo.mapper.BookMapper;
import com.example.demo.projection.BookSummary;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final CategoryRepository categoryRepository;

    public BookService(BookRepository bookRepository, BookMapper bookMapper,
                       CategoryRepository categoryRepository) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
        this.categoryRepository = categoryRepository;
    }

    public BookResponse save(BookRequest request) {
        if (bookRepository.existsByTitle(request.getTitle())) {
            throw new DuplicateBookException("Title Already Exists");
        }

        Book book = bookMapper.toEntity(request);
        applyCategory(book, request.getCategoryId());
        return bookMapper.toResponse(bookRepository.save(book));
    }

    public Page<BookResponse> findAll(Pageable pageable) {
        return bookRepository.findAll(pageable).map(bookMapper::toResponse);
    }

    public BookResponse findById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFound("No Book Found"));

        return bookMapper.toResponse(book);
    }

    public BookResponse update(Long id, BookRequest request) {
        Book existing = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFound("No Book Found"));

        existing.setTitle(request.getTitle());
        existing.setAuthor(request.getAuthor());
        existing.setPrice(request.getPrice());
        existing.setAvailable(request.isAvailable());
        applyCategory(existing, request.getCategoryId());

        return bookMapper.toResponse(bookRepository.save(existing));
    }

    private void applyCategory(Book book, Long categoryId) {
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new CategoryNotFoundException("No Category Found with id " + categoryId));
            book.setCategory(category);
        }
    }

    public List<BookSummary> getSummaries() {
        return bookRepository.findAllSummaries();
    }

    public void deleteById(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new BookNotFound("No Book Found");
        }

        bookRepository.deleteById(id);
    }

    public Page<BookResponse> findByAuthor(String author,Pageable pageable) {
        return bookRepository.findByAuthor(author,pageable).map(bookMapper::toResponse);
    }

    public Page<BookResponse> findByPriceLessThan(double price,Pageable pageable) {
        return bookRepository.findByPriceLessThan(price,pageable).map(bookMapper::toResponse);
    }

    public Page<BookResponse> findByAvailableTrue(Pageable pageable) {
        return bookRepository.findByAvailableTrue(pageable).map(bookMapper::toResponse);
    }

    public Page<BookResponse> findByTitleContaining(String title,Pageable pageable) {
        return bookRepository.findByTitleContaining(title,pageable).map(bookMapper::toResponse);
    }

    public Page<BookResponse> findByPriceBetween(double min, double max,Pageable pageable) {
        return bookRepository.findByPriceBetween(min, max, pageable).map(bookMapper::toResponse);
    }

}