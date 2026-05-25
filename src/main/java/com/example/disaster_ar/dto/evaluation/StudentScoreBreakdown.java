package com.example.disaster_ar.dto.evaluation;

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
public class StudentScoreBreakdown {

    private String scenarioId;
    private String studentId;

    private Double quiz;
    private Double role;
    private Double personal;
    private Double safezone;
    private Double total;

    private Integer correctQuizCount;

    private Boolean randomQuizCompleted;
    private Boolean reportCallCompleted;
    private Boolean extinguisherFound;
    private Boolean safeZoneCompleted;

    private Boolean fireteamExtinguisherAcquired;
    private Boolean fireteamExtinguisherQuizCompleted;
    private Boolean fireteamDonutCompleted;
}