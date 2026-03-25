package com.example.disaster_ar.dto.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
    private String classroomId;
    private String schoolId;
    private String className;
    private Integer studentCount;
    private String joinCode;
    private String trainingState;
}