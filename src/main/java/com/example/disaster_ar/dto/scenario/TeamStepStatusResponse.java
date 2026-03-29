package com.example.disaster_ar.dto.scenario;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamStepStatusResponse {

    private String scenarioId;
    private String assignmentId;
    private String teamId;

    private List<StepItem> steps;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepItem {
        private Integer stepOrder;
        private String stepType;
        private String status;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
    }
}