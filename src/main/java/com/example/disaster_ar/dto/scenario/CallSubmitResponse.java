package com.example.disaster_ar.dto.scenario;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallSubmitResponse {

    private String scenarioId;
    private String assignmentId;
    private String studentId;

    private List<String> selectedOrder;
    private List<String> correctOrder;

    private Boolean isCorrect;
    private Boolean missionCompleted;

    private Integer requiredCount;
    private Integer progressCount;
    private String status;

    private LocalDateTime submittedAt;
}