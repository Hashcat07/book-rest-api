package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request payload to create/update a book")
public class BookRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String author;

    @Positive
    private double price;

    private boolean available;

    private Long categoryId;
}
