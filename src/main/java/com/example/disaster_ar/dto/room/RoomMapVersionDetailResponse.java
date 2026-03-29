package com.example.disaster_ar.dto.room;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomMapVersionDetailResponse {
    private String mapVersionId;
    private String classroomId;
    private String schoolId;
    private String label;
    private String floorsJson;
    private String createdByUserId;
    private LocalDateTime createdAt;
    private Boolean isActive;
}