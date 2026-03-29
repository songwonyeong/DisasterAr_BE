package com.example.disaster_ar.dto.scenario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioActionEventResponse {
    private String actionEventId;
    private String scenarioId;
    private String studentId;
    private String actionType;
    private LocalDateTime createdAt;
}