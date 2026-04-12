package com.example.disaster_ar.dto.scenario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimulateBeaconDetectRequest {
    private String classroomId;
    private String studentId;
    private String beaconId;
    private Integer rssi;
    private Boolean updateLocation;
    private Boolean saveEvent;
    private String note;
}