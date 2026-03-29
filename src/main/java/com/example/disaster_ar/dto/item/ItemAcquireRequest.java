package com.example.disaster_ar.dto.item;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemAcquireRequest {
    private String itemId;
    private Integer quantity;          // null이면 1로 처리
    private String acquiredSource;     // SYSTEM / MISSION / TEACHER / QUIZ
}