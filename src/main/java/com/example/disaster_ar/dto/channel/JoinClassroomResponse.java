package com.example.disaster_ar.dto.channel;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinClassroomResponse {

    private String studentId;
    private String classroomId;
    private String className;
    private String trainingState;
    private LocalDateTime joinedAt;
}
