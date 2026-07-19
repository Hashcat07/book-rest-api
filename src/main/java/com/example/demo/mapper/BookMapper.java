package com.example.demo.mapper;

import com.example.demo.dto.BookRequest;
import com.example.demo.dto.BookResponse;
import com.example.demo.entity.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    Book toEntity(BookRequest req);

    @Mapping(source = "category.name", target = "categoryName")
    BookResponse toResponse(Book book);

    List<BookResponse> toResponseList(List<Book> books);
}