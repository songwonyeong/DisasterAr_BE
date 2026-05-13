package com.example.disaster_ar.dto.scenario;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtinguisherQuizSubmitResponse {

    private String scenarioId;
    private String assignmentId;
    private String studentId;

    private List<String> selectedOrder;

    private Boolean isCorrect;
    private Boolean missionCompleted;

    private Integer life;
    private Integer remainingLife;
    private Integer cooldownSeconds;

    private Integer requiredCount;
    private Integer progressCount;
    private String status;

    private String nextMissionCode;

    private LocalDateTime submittedAt;
}