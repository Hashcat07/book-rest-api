package com.example.demo.hateoas;

import com.example.demo.controller.BookController;
import com.example.demo.dto.BookResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class BookHateoasCreator {

    public EntityModel<BookResponse> addLinks(BookResponse bookResponse){

        EntityModel<BookResponse> response=EntityModel.of(bookResponse);
        response.add(linkTo(methodOn(BookController.class).getById(bookResponse.getId())).withSelfRel());
        response.add(linkTo(methodOn(BookController.class).getAllBooks(null)).withRel("All"));
        response.add(linkTo(methodOn(BookController.class).deleteBook(bookResponse.getId())).withRel("Delete"));
        response.add(linkTo(methodOn(BookController.class).updateById(bookResponse.getId(),null)).withRel("Update"));
        return response;
    }

    public List<EntityModel<BookResponse>> addLinksToList(List<BookResponse> bookResponses) {
        return bookResponses.stream()
                .map(this::addLinks)
                .toList();
    }

}
