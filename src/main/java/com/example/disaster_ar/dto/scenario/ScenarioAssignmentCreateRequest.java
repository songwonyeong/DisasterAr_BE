package com.example.disaster_ar.dto.scenario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScenarioAssignmentCreateRequest {
    private String scenarioId;
    private String classroomId;

    private String assignmentType; // QUIZ / MISSION / ROLE
    private String contentId;

    private String targetType; // TEAM / STUDENT / ALL
    private String targetTeamId;

    private Integer floorIndex;
    private String elementId;
    private String beaconId;

    private String paramsJson;

    private String createdByType;   // USER / SYSTEM / AI
    private String createdByUserId;
}