package com.example.disaster_ar.dto.scenario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BeaconDetectRequest {
    private String studentId;
    private String beaconId;
    private Integer rssi;
}