package com.example.demo.controller;

import com.example.demo.entity.Category;
import com.example.demo.repository.CategoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @PostMapping
    public ResponseEntity<Category> create(@RequestBody @Valid Category category) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryRepository.save(category));
    }

    @GetMapping
    public List<Category> all() {
        return categoryRepository.findAll();
    }
}
