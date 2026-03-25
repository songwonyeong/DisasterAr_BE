package com.example.disaster_ar.dto.room;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class TrainingStatusResponse {
    private String classroomId;
    private String studentId;
    private String trainingState;
    private LocalDateTime trainingStartedAt;
    private LocalDateTime trainingEndedAt;
    private String activeScenarioId;
    private Boolean isKicked;
}
