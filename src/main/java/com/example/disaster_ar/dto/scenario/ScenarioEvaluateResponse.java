package com.example.disaster_ar.dto.scenario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioEvaluateResponse {
    private String scenarioId;
    private Double scenarioScore;
    private Integer evaluatedStudentCount;
    private List<StudentEvaluationItem> studentEvaluations;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentEvaluationItem {
        private String studentId;
        private Double scoreTotal;
        private String feedbackText;
    }
}