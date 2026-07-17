package com.example.demo.mapper;

import com.example.demo.dto.BorrowResponse;
import com.example.demo.entity.BorrowRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BorrowMapper {

    @Mapping(source="user.name", target = "userName")
    @Mapping(source = "book.title", target = "bookTitle")
    BorrowResponse toResponse(BorrowRecord record);

    List<BorrowResponse> toResponseList(List<BorrowRecord> list);
}
