package com.example.disaster_ar.dto.ai;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiFeedbackRequest {

    private String studentName;
    private List<MissionItem> missions;
    private List<QuizItem> quizzes;
    private Boolean call119;

    @Getter
    @Setter
    public static class MissionItem {
        private String title;
        private String status;
    }

    @Getter
    @Setter
    public static class QuizItem {
        private Boolean isCorrect;
    }
}