package com.example.disaster_ar.dto.monitoring;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeaconMarkerResponse {

    private String beaconId;
    private Integer beaconNo;

    private String elementId;
    private String placementName;
    private String zoneType;

    private Double x;
    private Double y;
    private Double width;
    private Double height;

    private Integer studentCount;
    private List<MonitoringStudentResponse> students;
}