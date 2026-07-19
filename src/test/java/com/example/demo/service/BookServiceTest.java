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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private CategoryRepository categoryRepository;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, bookMapper, categoryRepository);
    }

    // --- helpers: build via setters so tests survive future field additions ---
    private Book book(Long id, String title, String author, double price, boolean available) {
        Book b = new Book();
        b.setId(id);
        b.setTitle(title);
        b.setAuthor(author);
        b.setPrice(price);
        b.setAvailable(available);
        return b;
    }

    private BookResponse response(Long id, String title, String author, double price, boolean available) {
        BookResponse r = new BookResponse();
        r.setId(id);
        r.setTitle(title);
        r.setAuthor(author);
        r.setPrice(price);
        r.setAvailable(available);
        return r;
    }

    @Test
    void testSave() {
        BookRequest request = new BookRequest("Java", "Author", 500, true, null);
        Book entity = book(null, "Java", "Author", 500, true);
        Book saved = book(1L, "Java", "Author", 500, true);
        BookResponse response = response(1L, "Java", "Author", 500, true);

        when(bookRepository.existsByTitle("Java")).thenReturn(false);
        when(bookMapper.toEntity(request)).thenReturn(entity);
        when(bookRepository.save(entity)).thenReturn(saved);
        when(bookMapper.toResponse(saved)).thenReturn(response);

        BookResponse result = bookService.save(request);

        assertEquals("Java", result.getTitle());
        assertEquals(1L, result.getId());
        verify(categoryRepository, never()).findById(any());
        verify(bookRepository).save(entity);
    }

    @Test
    void testSaveWithCategory() {
        BookRequest request = new BookRequest("Java", "Author", 500, true, 7L);
        Book entity = book(null, "Java", "Author", 500, true);
        Category category = new Category(7L, "Programming");
        Book saved = book(1L, "Java", "Author", 500, true);
        BookResponse response = response(1L, "Java", "Author", 500, true);

        when(bookRepository.existsByTitle("Java")).thenReturn(false);
        when(bookMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(category));
        when(bookRepository.save(entity)).thenReturn(saved);
        when(bookMapper.toResponse(saved)).thenReturn(response);

        bookService.save(request);

        assertEquals(category, entity.getCategory()); // category was attached before save
        verify(categoryRepository).findById(7L);
        verify(bookRepository).save(entity);
    }

    @Test
    void testSaveWithInvalidCategory() {
        BookRequest request = new BookRequest("Java", "Author", 500, true, 99L);
        Book entity = book(null, "Java", "Author", 500, true);

        when(bookRepository.existsByTitle("Java")).thenReturn(false);
        when(bookMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> bookService.save(request));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void testSaveDuplicate() {
        BookRequest request = new BookRequest("Java", "Author", 500, true, null);

        when(bookRepository.existsByTitle("Java")).thenReturn(true);

        assertThrows(DuplicateBookException.class, () -> bookService.save(request));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void testFindAll() {
        Pageable pageable = PageRequest.of(0, 5);
        Book book1 = book(1L, "Java", "James", 500.0, true);
        Book book2 = book(2L, "Spring", "Rod", 700.0, false);
        Page<Book> bookPage = new PageImpl<>(List.of(book1, book2), pageable, 2);

        when(bookRepository.findAll(pageable)).thenReturn(bookPage);
        when(bookMapper.toResponse(book1)).thenReturn(response(1L, "Java", "James", 500.0, true));
        when(bookMapper.toResponse(book2)).thenReturn(response(2L, "Spring", "Rod", 700.0, false));

        Page<BookResponse> result = bookService.findAll(pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(bookRepository).findAll(pageable);
    }

    @Test
    void testFindById() {
        Book b = book(1L, "Java", "James", 500.0, true);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(b));
        when(bookMapper.toResponse(b)).thenReturn(response(1L, "Java", "James", 500.0, true));

        BookResponse result = bookService.findById(1L);

        assertNotNull(result);
        assertEquals("Java", result.getTitle());
        verify(bookRepository).findById(1L);
    }

    @Test
    void testFindByIdNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BookNotFound.class, () -> bookService.findById(99L));
        verify(bookRepository).findById(99L);
    }

    @Test
    void testUpdate() {
        BookRequest request = new BookRequest("Updated Java", "Updated Author", 900.0, false, null);
        Book existing = book(1L, "Java", "James", 500.0, true);
        Book updated = book(1L, "Updated Java", "Updated Author", 900.0, false);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenReturn(updated);
        when(bookMapper.toResponse(updated)).thenReturn(response(1L, "Updated Java", "Updated Author", 900.0, false));

        BookResponse result = bookService.update(1L, request);

        assertEquals(900.0, result.getPrice());
        assertFalse(result.isAvailable());
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void testUpdateNotFound() {
        BookRequest request = new BookRequest("Java", "James", 500.0, true, null);

        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BookNotFound.class, () -> bookService.update(99L, request));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void testDelete() {
        when(bookRepository.existsById(1L)).thenReturn(true);

        bookService.deleteById(1L);

        verify(bookRepository).deleteById(1L);
    }

    @Test
    void testDeleteNotFound() {
        when(bookRepository.existsById(99L)).thenReturn(false);

        assertThrows(BookNotFound.class, () -> bookService.deleteById(99L));
        verify(bookRepository, never()).deleteById(anyLong());
    }

    @Test
    void testGetSummaries() {
        BookSummary summary = mock(BookSummary.class);
        when(summary.getTitle()).thenReturn("Java");
        when(summary.getAverageRating()).thenReturn(4.5);
        when(bookRepository.findAllSummaries()).thenReturn(List.of(summary));

        List<BookSummary> result = bookService.getSummaries();

        assertEquals(1, result.size());
        assertEquals("Java", result.get(0).getTitle());
        assertEquals(4.5, result.get(0).getAverageRating());
        verify(bookRepository).findAllSummaries();
    }

    @Test
    void testFindByAuthor() {
        Pageable pageable = PageRequest.of(0, 5);
        Book b = book(1L, "Java Basics", "James", 300.0, true);
        Page<Book> bookPage = new PageImpl<>(List.of(b), pageable, 1);

        when(bookRepository.findByAuthor("James", pageable)).thenReturn(bookPage);
        when(bookMapper.toResponse(b)).thenReturn(response(1L, "Java Basics", "James", 300.0, true));

        Page<BookResponse> result = bookService.findByAuthor("James", pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("James", result.getContent().get(0).getAuthor());
        verify(bookRepository).findByAuthor("James", pageable);
    }

    @Test
    void testFindByPriceLessThan() {
        Pageable pageable = PageRequest.of(0, 5);
        Book b = book(1L, "Java", "James", 300.0, true);
        Page<Book> bookPage = new PageImpl<>(List.of(b), pageable, 1);

        when(bookRepository.findByPriceLessThan(800.0, pageable)).thenReturn(bookPage);
        when(bookMapper.toResponse(b)).thenReturn(response(1L, "Java", "James", 300.0, true));

        Page<BookResponse> result = bookService.findByPriceLessThan(800.0, pageable);

        assertNotNull(result);
        assertTrue(result.getContent().get(0).getPrice() < 800.0);
        verify(bookRepository).findByPriceLessThan(800.0, pageable);
    }

    @Test
    void testFindByAvailableTrue() {
        Pageable pageable = PageRequest.of(0, 5);
        Book b = book(1L, "Java", "James", 300.0, true);
        Page<Book> bookPage = new PageImpl<>(List.of(b), pageable, 1);

        when(bookRepository.findByAvailableTrue(pageable)).thenReturn(bookPage);
        when(bookMapper.toResponse(b)).thenReturn(response(1L, "Java", "James", 300.0, true));

        Page<BookResponse> result = bookService.findByAvailableTrue(pageable);

        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(BookResponse::isAvailable));
        verify(bookRepository).findByAvailableTrue(pageable);
    }

    @Test
    void testFindByTitleContaining() {
        Pageable pageable = PageRequest.of(0, 5);
        Book b = book(4L, "Hibernate", "Gavin", 900.0, true);
        Page<Book> bookPage = new PageImpl<>(List.of(b), pageable, 1);

        when(bookRepository.findByTitleContaining("Hibernate", pageable)).thenReturn(bookPage);
        when(bookMapper.toResponse(b)).thenReturn(response(4L, "Hibernate", "Gavin", 900.0, true));

        Page<BookResponse> result = bookService.findByTitleContaining("Hibernate", pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("Hibernate", result.getContent().get(0).getTitle());
        verify(bookRepository).findByTitleContaining("Hibernate", pageable);
    }

    @Test
    void testFindByPriceBetween() {
        Pageable pageable = PageRequest.of(0, 5);
        Book b = book(1L, "Java", "James", 300.0, true);
        Page<Book> bookPage = new PageImpl<>(List.of(b), pageable, 1);

        when(bookRepository.findByPriceBetween(200.0, 600.0, pageable)).thenReturn(bookPage);
        when(bookMapper.toResponse(b)).thenReturn(response(1L, "Java", "James", 300.0, true));

        Page<BookResponse> result = bookService.findByPriceBetween(200.0, 600.0, pageable);

        assertEquals(1, result.getContent().size());
        verify(bookRepository).findByPriceBetween(200.0, 600.0, pageable);
    }

}
