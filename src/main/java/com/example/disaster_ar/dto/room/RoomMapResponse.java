package com.example.disaster_ar.dto.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomMapResponse {
    private String mapVersionId;
    private String classroomId;
    private String schoolId;
    private String label;
    private String floorsJson;
    private LocalDateTime createdAt;
}