package com.example.disaster_ar.dto.scenario;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtinguisherQuizResultResponse {

    private String scenarioId;
    private String assignmentId;
    private String studentId;

    private String missionCode;

    private Boolean isCorrect;
    private Boolean missionCompleted;

    private Integer remainingLife;
    private Integer attemptCount;

    private Integer requiredCount;
    private Integer progressCount;
    private String status;

    private String nextMissionCode;

    private LocalDateTime submittedAt;
}