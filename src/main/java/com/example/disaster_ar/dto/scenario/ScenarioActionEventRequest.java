package com.example.disaster_ar.dto.scenario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScenarioActionEventRequest {
    private String classroomId;
    private String studentId;
    private String actionType;
    private Integer floorIndex;
    private String elementId;
    private String beaconId;
    private Integer valueInt;
    private String valueText;
    private String metaJson;
}