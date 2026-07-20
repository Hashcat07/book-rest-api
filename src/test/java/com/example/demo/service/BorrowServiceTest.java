package com.example.demo.service;

import com.example.demo.dto.BookResponse;
import com.example.demo.entity.Book;
import com.example.demo.entity.BorrowRecord;
import com.example.demo.entity.User;
import com.example.demo.exception.BookNotAvailableException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.mapper.BookMapper;
import com.example.demo.mapper.BorrowMapper;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.BorrowRecordRepository;
import com.example.demo.repository.UserRepository;
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
class BorrowServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BorrowRecordRepository borrowRecordRepository;

    @Mock
    private BorrowMapper borrowMapper;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BorrowService borrowService;

    private static final String EMAIL = "rohul@example.com";

    private User user(Long id) {
        User u = new User();
        u.setId(id);
        u.setName("Rohul");
        u.setEmail(EMAIL);
        return u;
    }

    private Book book(Long id, boolean available) {
        Book b = new Book();
        b.setId(id);
        b.setAvailable(available);
        return b;
    }

    @Test
    void testBorrowSuccess() {
        Book book = book(1L, true);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(1L)));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        String result = borrowService.borrowBook(EMAIL, 1L);

        assertEquals("Book Borrowed", result);
        assertFalse(book.isAvailable()); // book flipped to unavailable
        verify(bookRepository).save(book);
        verify(borrowRecordRepository).save(any(BorrowRecord.class));
    }

    @Test
    void testBorrowAlreadyBorrowed() {
        Book book = book(1L, false); // already out

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(1L)));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        assertThrows(BookNotAvailableException.class, () -> borrowService.borrowBook(EMAIL, 1L));
        verify(borrowRecordRepository, never()).save(any());
    }

    @Test
    void testBorrowUserNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> borrowService.borrowBook(EMAIL, 1L));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void testReturnSuccess() {
        Book book = book(1L, false);
        BorrowRecord record = new BorrowRecord();
        record.setBook(book);

        when(borrowRecordRepository.findByBookIdAndReturnDateIsNull(1L)).thenReturn(Optional.of(record));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookMapper.toResponse(book)).thenReturn(new BookResponse());

        borrowService.returnBook(1L);

        assertNotNull(record.getReturnDate()); // return stamped
        assertTrue(book.isAvailable());        // book available again
        verify(borrowRecordRepository).save(record);
    }
}
