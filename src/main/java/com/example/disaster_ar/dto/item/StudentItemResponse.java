package com.example.disaster_ar.dto.item;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentItemResponse {
    private String itemId;
    private String itemCode;
    private String itemName;
    private Integer quantity;
    private Boolean isConsumed;
    private LocalDateTime acquiredAt;
    private LocalDateTime consumedAt;
}