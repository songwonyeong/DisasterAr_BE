package com.example.disaster_ar.dto.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentKickResponse {
    private String studentId;
    private String classroomId;
    private Boolean isKicked;
    private String message;
}