package com.example.disaster_ar.dto.scenario;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionCompleteResponse {
    private String scenarioId;
    private String assignmentId;
    private String studentId;
    private String status;
    private LocalDateTime completedAt;
}