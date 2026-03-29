package com.example.disaster_ar.dto.scenario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardQuizSubmitRequest {
    private String studentId;
    private String quizKind; // FIRE / EMERGENCY / OTHER
    private String selectedOrderJson;
}