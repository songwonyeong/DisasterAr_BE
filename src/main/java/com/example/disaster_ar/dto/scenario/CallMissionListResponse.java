package com.example.disaster_ar.dto.scenario;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallMissionListResponse {
    private String scenarioId;
    private String trainingSessionId;
    private List<StudentCallMissionResponse> missions;
}
