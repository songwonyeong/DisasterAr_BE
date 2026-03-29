package com.example.disaster_ar.dto.scenario;

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
public class QuizSubmitResponse {
    private String scenarioId;
    private String assignmentId;
    private String studentId;
    private Integer selectedAnswer;
    private Integer correctAnswer;
    private Boolean isCorrect;
    private String status;
    private LocalDateTime submittedAt;
}