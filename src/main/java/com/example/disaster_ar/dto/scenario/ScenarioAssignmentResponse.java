package com.example.disaster_ar.dto.scenario;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScenarioAssignmentResponse {
    private String id;
    private String scenarioId;
    private String classroomId;

    private String assignmentType;
    private String contentId;

    private String targetType;
    private String targetTeamId;

    private Integer floorIndex;
    private String elementId;
    private String beaconId;

    private String paramsJson;

    private String createdByType;
    private String createdByUserId;
}