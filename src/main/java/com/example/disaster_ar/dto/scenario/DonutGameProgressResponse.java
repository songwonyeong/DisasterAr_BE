package com.example.disaster_ar.dto.scenario;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonutGameProgressResponse {

    private String scenarioId;
    private String assignmentId;
    private String teamId;
    private String teamCode;
    private String studentId;

    private String missionCode;

    private Integer requiredCount;
    private Integer progressCount;
    private Integer incrementCount;

    private String status;
    private Boolean missionCompleted;

    private Integer acceptedIncrementCount;
    private String phase;
    private String nextClientAction;
    private Integer waitRemainingSeconds;
}