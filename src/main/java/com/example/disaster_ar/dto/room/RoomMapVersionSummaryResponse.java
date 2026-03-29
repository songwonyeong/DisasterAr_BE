package com.example.disaster_ar.dto.room;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomMapVersionSummaryResponse {
    private String mapVersionId;
    private String label;
    private LocalDateTime createdAt;
    private String createdByUserId;
    private Boolean isActive;
}