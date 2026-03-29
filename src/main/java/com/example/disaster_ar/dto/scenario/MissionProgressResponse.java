package com.example.disaster_ar.dto.scenario;

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
public class MissionProgressResponse {
    private String scenarioId;
    private String assignmentId;
    private String studentId;
    private Integer requiredCount;
    private Integer progressCount;
    private String status;
}