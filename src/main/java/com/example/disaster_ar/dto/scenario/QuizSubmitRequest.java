package com.example.disaster_ar.dto.scenario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizSubmitRequest {
    private String studentId;

    // /random-quiz 응답으로 받은 contentId
    private String contentId;

    // 4지선다: 1~4, OX: O=1, X=2
    private Integer selectedAnswer;
}