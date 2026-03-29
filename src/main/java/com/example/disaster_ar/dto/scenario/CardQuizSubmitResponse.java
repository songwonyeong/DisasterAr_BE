package com.example.disaster_ar.dto.scenario;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardQuizSubmitResponse {
    private String scenarioId;
    private String assignmentId;
    private String studentId;
    private String quizKind;
    private Boolean isCorrect;
    private LocalDateTime submittedAt;
}