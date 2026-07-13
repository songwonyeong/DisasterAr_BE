package com.example.disaster_ar.dto.scenario;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCallMissionResponse {
    private String missionId;
    private String scenarioId;
    private String classroomId;
    private String trainingSessionId;
    private String studentId;
    private String studentName;
    private String assignmentId;
    private Integer triggerOffsetSeconds;
    private LocalDateTime availableFrom;
    private String status;
    private Boolean callAvailable;
    private Long remainingSeconds;
    private LocalDateTime callStartedAt;
    private LocalDateTime callEndedAt;
    private LocalDateTime teacherJudgedAt;
    private String teacherJudgementMemo;
}
