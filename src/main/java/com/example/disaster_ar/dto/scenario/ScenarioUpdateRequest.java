package com.example.disaster_ar.dto.scenario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScenarioUpdateRequest {

    private String scenarioId;

    private String scenarioName;

    private String scenarioType;
    private String triggerMode;
    private String teamMode;
    private String npcMode;

    private String location;
    private Integer intensity;
    private Integer trainTime;

    private String teamAssignment;
    private String npcPositions;

    private Integer participantCount;
}
