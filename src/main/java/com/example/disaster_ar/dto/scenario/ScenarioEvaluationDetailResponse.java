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
        private String scoreJson;
        private String detailsJson;
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

        /*
         * 기존 프론트 호환용으로 유지.
         * 새 프론트는 위의 직접 필드를 쓰면 됨.
         */
        private String scoreJson;
        private String detailsJson;

        private String feedbackText;
        private LocalDateTime createdAt;
    }
}