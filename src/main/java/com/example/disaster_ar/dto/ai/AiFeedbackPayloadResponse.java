package com.example.disaster_ar.dto.ai;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFeedbackPayloadResponse {

    private String studentId;
    private String studentName;
    private String role;

    private List<MissionResult> missions;
    private List<QuizResult> quizResults;

    private Boolean reportCallCompleted;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissionResult {
        private String missionType;
        private Boolean completed;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizResult {
        private String quizId;
        private Boolean correct;
    }
}