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
public class TrainingControlResponse {
    private String classroomId;
    private String trainingState;
    private LocalDateTime trainingStartedAt;
    private LocalDateTime trainingEndedAt;
    private String activeScenarioId;
}