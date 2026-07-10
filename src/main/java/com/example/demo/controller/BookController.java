package com.example.demo.controller;

import com.example.demo.dto.BookRequest;
import com.example.demo.dto.BookResponse;
import com.example.demo.hateoas.BookHateoasCreator;
import com.example.demo.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/books")
@Slf4j
public class BookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookHateoasCreator hateoasCreator;

    @PostMapping
    public ResponseEntity<EntityModel<BookResponse>> addBook(
            @RequestBody @Valid BookRequest book) {

        BookResponse newBook = bookService.save(book);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hateoasCreator.addLinks(newBook));
    }

    @GetMapping
    @Operation(summary = "Get All Books", description = "Returns all the books in the DB")
    public ResponseEntity<Page<EntityModel<BookResponse>>> getAllBooks(Pageable pageable) {
        return ResponseEntity.ok().body(bookService.findAll(pageable).map(hateoasCreator::addLinks));
    }

    @GetMapping(value = "/{id}", produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE
    })
    @Operation(summary = "Get Books based on a ID", description = "Returns a single book, or 404 if not found")
    public ResponseEntity<EntityModel<BookResponse>> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                hateoasCreator.addLinks(bookService.findById(id))
        );
    }

    @PutMapping(value = "/{id}", produces = {
            MediaType.APPLICATION_ATOM_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE
    })
    public ResponseEntity<EntityModel<BookResponse>> updateById(
            @PathVariable Long id,
            @RequestBody @Valid BookRequest book) {

        return ResponseEntity.ok(
                hateoasCreator.addLinks(bookService.update(id, book))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable Long id) {
        bookService.deleteById(id);
        return ResponseEntity.ok("Done");
    }

    @GetMapping("/author")
    public ResponseEntity<Page<EntityModel<BookResponse>>> getByAuthor(
            @RequestParam String author,Pageable pageable) {

        return ResponseEntity.ok().body(bookService.findByAuthor(author,pageable).map(hateoasCreator::addLinks));
    }

    @GetMapping("/price")
    public ResponseEntity<Page<EntityModel<BookResponse>>> getByPriceLessThan(
            @RequestParam double price,Pageable pageable) {

        return ResponseEntity.ok(bookService.findByPriceLessThan(price,pageable).map(hateoasCreator::addLinks));
    }

    @GetMapping("/available")
    public ResponseEntity<Page<EntityModel<BookResponse>>> getAvailableBooks(Pageable pageable) {

        return ResponseEntity.ok(bookService.findByAvailableTrue(pageable).map(hateoasCreator::addLinks));
    }

    @GetMapping("/title")
    public ResponseEntity<Page<EntityModel<BookResponse>>> getByTitle(
            @RequestParam String title, Pageable pageable) {

        return ResponseEntity.ok(bookService.findByTitleContaining(title, pageable).map(hateoasCreator::addLinks));

    }

    @GetMapping("/between")
    public ResponseEntity<Page<EntityModel<BookResponse>>> getByPriceBetween(
            @RequestParam double min,
            @RequestParam double max,
            Pageable pageable) {

        return ResponseEntity.ok(bookService.findByPriceBetween(min, max, pageable).map(hateoasCreator::addLinks));

    }

}