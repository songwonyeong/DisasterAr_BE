package com.example.disaster_ar.dto.scenario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioEvaluationDetailResponse {
    private String scenarioId;
    private EvaluationSummary scenarioEvaluation;
    private List<StudentEvaluationSummary> studentEvaluations;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationSummary {
        private String evaluationId;
        private Double scoreTotal;
        private String feedbackText;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentEvaluationSummary {
        private String evaluationId;
        private String studentId;
        private Double scoreTotal;
        private String feedbackText;
        private LocalDateTime createdAt;
    }
}