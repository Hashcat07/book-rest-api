package com.example.demo.mapper;

import com.example.demo.dto.ReviewRequest;
import com.example.demo.dto.ReviewResponse;
import com.example.demo.entity.Review;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    Review toEntity(ReviewRequest reviewRequest);
    ReviewResponse toResponse(Review review);

}
