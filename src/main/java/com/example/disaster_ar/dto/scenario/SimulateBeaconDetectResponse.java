package com.example.disaster_ar.dto.scenario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class SimulateBeaconDetectResponse {
    private String scenarioId;
    private String classroomId;
    private String studentId;
    private String beaconId;
    private boolean locationUpdated;
    private boolean eventSaved;
    private List<String> triggeredAssignmentIds;
    private LocalDateTime eventAt;
    private String message;
}