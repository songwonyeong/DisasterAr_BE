package com.example.disaster_ar.dto.scenario;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ScenarioResponse {

    private String id;
    private String classroomId;

    private String scenarioName;

    private String scenarioType;
    private String triggerMode;
    private String teamMode;
    private String npcMode;

    private String location;
    private Integer intensity;
    private Integer trainTime;

    private String teamAssignmentJson;
    private String npcPositionsJson;

    private Integer participantCount;
    private LocalDateTime createdTime;
}
