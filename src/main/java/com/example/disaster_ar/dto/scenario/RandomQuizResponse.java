package com.example.disaster_ar.dto.scenario;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RandomQuizResponse {

    private String contentId;
    private String quizType;

    private String question;

    private String option1;
    private String option2;
    private String option3;
    private String option4;
}