package com.example.demo.controller;

import com.example.demo.dto.BookRequest;
import com.example.demo.dto.BookResponse;
import com.example.demo.exception.BookNotFound;
import com.example.demo.exception.DuplicateBookException;
import com.example.demo.hateoas.BookHateoasCreator;
import com.example.demo.service.BookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
public class BookControllerTest {

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private BookHateoasCreator hateoasCreator;

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testSave() throws Exception {
        BookRequest request = new BookRequest("Java", "James", 500.0, true, null);
        BookResponse response = new BookResponse(1L, "Java", "James", 500.0, true, null, null, null);
        EntityModel<BookResponse> entityModel = EntityModel.of(response);

        when(bookService.save(any(BookRequest.class))).thenReturn(response);
        when(hateoasCreator.addLinks(response)).thenReturn(entityModel);

        mockMvc.perform(
                        post("/books")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Java"))
                .andExpect(jsonPath("$.author").value("James"))
                .andExpect(jsonPath("$.price").value(500.0));
    }

    @Test
    void testPostValidationFailure() throws Exception {
        BookRequest request = new BookRequest("", "Author", -10, true, null);

        mockMvc.perform(
                        post("/books")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPostDuplicateTitle() throws Exception {
        BookRequest request = new BookRequest("Java", "James", 500.0, true, null);

        when(bookService.save(any(BookRequest.class)))
                .thenThrow(new DuplicateBookException("Title Already Exists"));

        mockMvc.perform(
                        post("/books")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void testGetAllBooks() throws Exception {
        Pageable pageable = PageRequest.of(0, 5);
        BookResponse book1 = new BookResponse(1L, "Java", "James", 500.0, true, null, null, null);
        BookResponse book2 = new BookResponse(2L, "Spring", "Rod", 700.0, false, null, null, null);

        Page<BookResponse> bookPage = new PageImpl<>(List.of(book1, book2), pageable, 2);

        when(bookService.findAll(any(Pageable.class))).thenReturn(bookPage);
        when(hateoasCreator.addLinks(book1)).thenReturn(EntityModel.of(book1));
        when(hateoasCreator.addLinks(book2)).thenReturn(EntityModel.of(book2));

        mockMvc.perform(get("/books?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Java"))
                .andExpect(jsonPath("$.content[1].title").value("Spring"));
    }

    @Test
    void testGetById() throws Exception {
        BookResponse response = new BookResponse(1L, "Java", "James", 500.0, true, null, null, null);
        EntityModel<BookResponse> entityModel = EntityModel.of(response);

        when(bookService.findById(1L)).thenReturn(response);
        when(hateoasCreator.addLinks(response)).thenReturn(entityModel);

        mockMvc.perform(get("/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java"))
                .andExpect(jsonPath("$.author").value("James"));
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        when(bookService.findById(anyLong()))
                .thenThrow(new BookNotFound("No Book Found"));

        mockMvc.perform(get("/books/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateBook() throws Exception {
        BookRequest request = new BookRequest("Java Updated", "James Updated", 700.0, false, null);
        BookResponse response = new BookResponse(1L, "Java Updated", "James Updated", 700.0, false, null, null, null);
        EntityModel<BookResponse> entityModel = EntityModel.of(response);

        when(bookService.update(anyLong(), any(BookRequest.class))).thenReturn(response);
        when(hateoasCreator.addLinks(response)).thenReturn(entityModel);

        mockMvc.perform(
                        put("/books/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteBook() throws Exception {
        mockMvc.perform(delete("/books/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetByAuthor() throws Exception {
        Pageable pageable = PageRequest.of(0, 5);
        BookResponse book = new BookResponse(1L, "Java", "James", 500.0, true, null, null, null);

        Page<BookResponse> bookPage = new PageImpl<>(List.of(book), pageable, 1);

        when(bookService.findByAuthor(eq("James"), any(Pageable.class))).thenReturn(bookPage);
        when(hateoasCreator.addLinks(book)).thenReturn(EntityModel.of(book));

        mockMvc.perform(get("/books/author?author=James&page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].author").value("James"));
    }

    @Test
    void testGetByPriceLessThan() throws Exception {
        Pageable pageable = PageRequest.of(0, 5);
        BookResponse book = new BookResponse(1L, "Java", "James", 500.0, true, null, null, null);

        Page<BookResponse> bookPage = new PageImpl<>(List.of(book), pageable, 1);

        when(bookService.findByPriceLessThan(eq(600.0), any(Pageable.class))).thenReturn(bookPage);
        when(hateoasCreator.addLinks(book)).thenReturn(EntityModel.of(book));

        mockMvc.perform(get("/books/price?price=600.0&page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void testGetAvailableBooks() throws Exception {
        Pageable pageable = PageRequest.of(0, 5);
        BookResponse book = new BookResponse(1L, "Java", "James", 500.0, true, null, null, null);

        Page<BookResponse> bookPage = new PageImpl<>(List.of(book), pageable, 1);

        when(bookService.findByAvailableTrue(any(Pageable.class))).thenReturn(bookPage);
        when(hateoasCreator.addLinks(book)).thenReturn(EntityModel.of(book));

        mockMvc.perform(get("/books/available?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void testGetByTitle() throws Exception {
        Pageable pageable = PageRequest.of(0, 5);
        BookResponse book = new BookResponse(1L, "Java", "James", 500.0, true, null, null, null);

        Page<BookResponse> bookPage = new PageImpl<>(List.of(book), pageable, 1);

        when(bookService.findByTitleContaining(eq("Java"), any(Pageable.class))).thenReturn(bookPage);
        when(hateoasCreator.addLinks(book)).thenReturn(EntityModel.of(book));

        mockMvc.perform(get("/books/title?title=Java&page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void testGetByPriceBetween() throws Exception {
        Pageable pageable = PageRequest.of(0, 5);
        BookResponse book = new BookResponse(1L, "Java", "James", 500.0, true, null, null, null);

        Page<BookResponse> bookPage = new PageImpl<>(List.of(book), pageable, 1);

        when(bookService.findByPriceBetween(eq(400.0), eq(600.0), any(Pageable.class))).thenReturn(bookPage);
        when(hateoasCreator.addLinks(book)).thenReturn(EntityModel.of(book));

        mockMvc.perform(get("/books/between?min=400&max=600&page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }
}
