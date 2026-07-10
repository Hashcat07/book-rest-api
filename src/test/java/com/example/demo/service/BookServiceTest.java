package com.example.demo.service;

import com.example.demo.dto.BookRequest;
import com.example.demo.dto.BookResponse;
import com.example.demo.entity.Book;
import com.example.demo.exception.BookNotFound;
import com.example.demo.exception.DuplicateBookException;
import com.example.demo.mapper.BookMapper;
import com.example.demo.repository.BookRepository;
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

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, bookMapper);
    }

    @Test
    void testSave() {
        BookRequest request = new BookRequest("Java", "Author", 500, true);
        Book entity = new Book(null, "Java", "Author", 500, true, null, null, null, null);
        Book saved = new Book(1L, "Java", "Author", 500, true, null, null, null, null);
        BookResponse response = new BookResponse(1L, "Java", "Author", 500, true, null, null);

        when(bookRepository.existsByTitle("Java")).thenReturn(false);
        when(bookMapper.toEntity(request)).thenReturn(entity);
        when(bookRepository.save(entity)).thenReturn(saved);
        when(bookMapper.toResponse(saved)).thenReturn(response);

        BookResponse result = bookService.save(request);

        assertEquals("Java", result.getTitle());
        assertEquals(1L, result.getId());
        verify(bookRepository).save(entity);
    }

    @Test
    void testSaveDuplicate() {
        BookRequest request = new BookRequest("Java", "Author", 500, true);

        when(bookRepository.existsByTitle("Java")).thenReturn(true);

        assertThrows(DuplicateBookException.class, () -> bookService.save(request));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void testFindAll() {
        Pageable pageable = PageRequest.of(0, 5);
        Book book1 = new Book(1L, "Java", "James", 500.0, true, null, null, null, null);
        Book book2 = new Book(2L, "Spring", "Rod", 700.0, false, null, null, null, null);
        Page<Book> bookPage = new PageImpl<>(List.of(book1, book2), pageable, 2);

        BookResponse resp1 = new BookResponse(1L, "Java", "James", 500.0, true, null, null);
        BookResponse resp2 = new BookResponse(2L, "Spring", "Rod", 700.0, false, null, null);

        when(bookRepository.findAll(pageable)).thenReturn(bookPage);
        when(bookMapper.toResponse(book1)).thenReturn(resp1);
        when(bookMapper.toResponse(book2)).thenReturn(resp2);

        Page<BookResponse> result = bookService.findAll(pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(bookRepository).findAll(pageable);
    }

    @Test
    void testFindById() {
        Book book = new Book(1L, "Java", "James", 500.0, true, null, null, null, null);
        BookResponse response = new BookResponse(1L, "Java", "James", 500.0, true, null, null);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookMapper.toResponse(book)).thenReturn(response);

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
        BookRequest request = new BookRequest("Updated Java", "Updated Author", 900.0, false);
        Book existing = new Book(1L, "Java", "James", 500.0, true, null, null, null, null);
        Book updated = new Book(1L, "Updated Java", "Updated Author", 900.0, false, null, null, null, null);
        BookResponse response = new BookResponse(1L, "Updated Java", "Updated Author", 900.0, false, null, null);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenReturn(updated);
        when(bookMapper.toResponse(updated)).thenReturn(response);

        BookResponse result = bookService.update(1L, request);

        assertEquals(900.0, result.getPrice());
        assertFalse(result.isAvailable());
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void testUpdateNotFound() {
        BookRequest request = new BookRequest("Java", "James", 500.0, true);

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
    void testFindByAuthor() {
        Pageable pageable = PageRequest.of(0, 5);
        Book book1 = new Book(1L, "Java Basics", "James", 300.0, true, null, null, null, null);
        Book book2 = new Book(3L, "Advanced Java", "James", 700.0, false, null, null, null, null);
        Page<Book> bookPage = new PageImpl<>(List.of(book1, book2), pageable, 2);

        BookResponse resp1 = new BookResponse(1L, "Java Basics", "James", 300.0, true, null, null);
        BookResponse resp2 = new BookResponse(3L, "Advanced Java", "James", 700.0, false, null, null);

        when(bookRepository.findByAuthor("James", pageable)).thenReturn(bookPage);
        when(bookMapper.toResponse(book1)).thenReturn(resp1);
        when(bookMapper.toResponse(book2)).thenReturn(resp2);

        Page<BookResponse> result = bookService.findByAuthor("James", pageable);

        assertEquals(2, result.getContent().size());
        assertEquals("James", result.getContent().get(0).getAuthor());
        verify(bookRepository).findByAuthor("James", pageable);
    }

    @Test
    void testFindByPriceLessThan() {
        Pageable pageable = PageRequest.of(0, 5);
        Book book1 = new Book(1L, "Java", "James", 300.0, true, null, null, null, null);
        Book book2 = new Book(2L, "Spring", "Rod", 500.0, true, null, null, null, null);
        Page<Book> bookPage = new PageImpl<>(List.of(book1, book2), pageable, 2);

        BookResponse resp1 = new BookResponse(1L, "Java", "James", 300.0, true, null, null);
        BookResponse resp2 = new BookResponse(2L, "Spring", "Rod", 500.0, true, null, null);

        when(bookRepository.findByPriceLessThan(800.0, pageable)).thenReturn(bookPage);
        when(bookMapper.toResponse(book1)).thenReturn(resp1);
        when(bookMapper.toResponse(book2)).thenReturn(resp2);

        Page<BookResponse> result = bookService.findByPriceLessThan(800.0, pageable);

        assertNotNull(result);
        assertTrue(result.getContent().get(0).getPrice() < 800.0);
        verify(bookRepository).findByPriceLessThan(800.0, pageable);
    }

    @Test
    void testFindByAvailableTrue() {
        Pageable pageable = PageRequest.of(0, 5);
        Book book1 = new Book(1L, "Java", "James", 300.0, true, null, null, null, null);
        Book book2 = new Book(4L, "Hibernate", "Gavin", 900.0, true, null, null, null, null);
        Page<Book> bookPage = new PageImpl<>(List.of(book1, book2), pageable, 2);

        BookResponse resp1 = new BookResponse(1L, "Java", "James", 300.0, true, null, null);
        BookResponse resp2 = new BookResponse(4L, "Hibernate", "Gavin", 900.0, true, null, null);

        when(bookRepository.findByAvailableTrue(pageable)).thenReturn(bookPage);
        when(bookMapper.toResponse(book1)).thenReturn(resp1);
        when(bookMapper.toResponse(book2)).thenReturn(resp2);

        Page<BookResponse> result = bookService.findByAvailableTrue(pageable);

        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(BookResponse::isAvailable));
        verify(bookRepository).findByAvailableTrue(pageable);
    }

    @Test
    void testFindByTitleContaining() {
        Pageable pageable = PageRequest.of(0, 5);
        Book book = new Book(4L, "Hibernate", "Gavin", 900.0, true, null, null, null, null);
        Page<Book> bookPage = new PageImpl<>(List.of(book), pageable, 1);

        BookResponse response = new BookResponse(4L, "Hibernate", "Gavin", 900.0, true, null, null);

        when(bookRepository.findByTitleContaining("Hibernate", pageable)).thenReturn(bookPage);
        when(bookMapper.toResponse(book)).thenReturn(response);

        Page<BookResponse> result = bookService.findByTitleContaining("Hibernate", pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("Hibernate", result.getContent().get(0).getTitle());
        verify(bookRepository).findByTitleContaining("Hibernate", pageable);
    }

    @Test
    void testFindByPriceBetween() {
        Pageable pageable = PageRequest.of(0, 5);
        Book book1 = new Book(1L, "Java", "James", 300.0, true, null, null, null, null);
        Book book2 = new Book(2L, "Spring", "Rod", 500.0, true, null, null, null, null);
        Page<Book> bookPage = new PageImpl<>(List.of(book1, book2), pageable, 2);

        BookResponse resp1 = new BookResponse(1L, "Java", "James", 300.0, true, null, null);
        BookResponse resp2 = new BookResponse(2L, "Spring", "Rod", 500.0, true, null, null);

        when(bookRepository.findByPriceBetween(200.0, 600.0, pageable)).thenReturn(bookPage);
        when(bookMapper.toResponse(book1)).thenReturn(resp1);
        when(bookMapper.toResponse(book2)).thenReturn(resp2);

        Page<BookResponse> result = bookService.findByPriceBetween(200.0, 600.0, pageable);

        assertEquals(2, result.getContent().size());
        verify(bookRepository).findByPriceBetween(200.0, 600.0, pageable);
    }

}
