package com.example.disaster_ar.dto.monitoring;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringMapResponse {

    private String classroomId;
    private String mapVersionId;

    private List<MonitoringFloorResponse> floors;
}