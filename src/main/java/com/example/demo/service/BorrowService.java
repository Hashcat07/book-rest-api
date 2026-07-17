package com.example.demo.service;

import com.example.demo.dto.BookResponse;
import com.example.demo.dto.BorrowResponse;
import com.example.demo.entity.Book;
import com.example.demo.entity.BorrowRecord;
import com.example.demo.entity.User;
import com.example.demo.exception.BookNotAvailableException;
import com.example.demo.exception.BookNotFound;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.mapper.BookMapper;
import com.example.demo.mapper.BorrowMapper;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.BorrowRecordRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BorrowService {
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final BorrowMapper borrowMapper;
    private final BookMapper bookMapper;
    @Transactional
    public String borrowBook(Long userId,Long bookId){
            User user = userRepository.findById(userId).orElseThrow(()-> new UserNotFoundException("User Not Found"));
            Book book = bookRepository.findById(bookId).orElseThrow(()-> new BookNotFound("Book Not Found"));

            if(!book.isAvailable()){
                throw new BookNotAvailableException("Book Already Borrowed");
            }
            book.setAvailable(false);
            bookRepository.save(book);
            BorrowRecord borrowRecord = new BorrowRecord();
            borrowRecord.setUser(user);
            borrowRecord.setBook(book);
            borrowRecord.setBorrowDate(LocalDateTime.now());
            borrowRecord.setDueDate(LocalDateTime.now().plusDays(14));
            borrowRecordRepository.save(borrowRecord);
            return "Book Borrowed";
    }

    @Transactional
    public BookResponse returnBook(Long bookId){
        BorrowRecord borrowRecord=borrowRecordRepository.findByBookIdAndReturnDateIsNull(bookId).orElseThrow(()-> new BookNotFound("Book Not Available"));
        borrowRecord.setReturnDate(LocalDateTime.now());
        borrowRecordRepository.save(borrowRecord);

        Book book=bookRepository.findById(bookId).orElseThrow(()-> new BookNotFound("Book Not Available"));
        book.setAvailable(true);
        bookRepository.save(book);
        return bookMapper.toResponse(book);
    }

    public List<BorrowResponse> getUserHistory(Long userId){
        User user= userRepository.findById(userId).orElseThrow(()-> new UserNotFoundException("User Not Found"));
        List<BorrowRecord> list= borrowRecordRepository.findByUserId(user.getId());
        return borrowMapper.toResponseList(list);

    }
}
