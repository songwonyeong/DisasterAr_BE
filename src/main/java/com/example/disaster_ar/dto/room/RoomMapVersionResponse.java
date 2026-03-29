package com.example.disaster_ar.dto.room;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomMapVersionResponse {
    private String mapVersionId;
    private String classroomId;
    private String label;
    private LocalDateTime createdAt;
}