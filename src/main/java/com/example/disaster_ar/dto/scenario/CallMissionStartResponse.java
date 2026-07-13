package com.example.disaster_ar.dto.scenario;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallMissionStartResponse {
    private String status; // NOT_AVAILABLE_YET, QUIZ_READY, CALL_READY
    private String mode;   // QUIZ, TEACHER_CALL
    private String message;
    private Long remainingSeconds;
    private String teacherPhoneNumber;
    private String quizApi;
}
