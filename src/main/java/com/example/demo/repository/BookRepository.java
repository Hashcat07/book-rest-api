package com.example.demo.repository;

import com.example.demo.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book,Long> {

    boolean existsByTitle(String title);
    Page<Book> findByAuthor(String author, Pageable pageable);
    Page<Book> findByPriceLessThan(double price, Pageable pageable);
    Page<Book> findByAvailableTrue(Pageable pageable);
    Page<Book> findByTitleContaining(String title, Pageable pageable);
    Page<Book> findByPriceBetween(double min,double max, Pageable pageable);

}
