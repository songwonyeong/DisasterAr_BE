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
        private String studentName;
        private Boolean isKicked;

        private String teamId;
        private String teamCode;
        private String teamName;

        private Double scoreTotal;

        private Double quizScore;
        private Double roleScore;
        private Double personalScore;
        private Double safezoneScore;
        private Double kickedPenalty;

        private Integer correctQuizCount;

        private Boolean randomQuizCompleted;
        private Boolean reportCallCompleted;
        private Boolean extinguisherFound;
        private Boolean safeZoneCompleted;

        private Boolean fireteamExtinguisherAcquired;
        private Boolean fireteamExtinguisherQuizCompleted;
        private Boolean fireteamDonutCompleted;

        private String feedbackText;
    }
}