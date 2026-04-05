package com.example.disaster_ar.dto.beacon;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationMessage {
    private String studentId;
    private String studentName;
    private int floorIndex;
    private double x;
    private double y;
    private String beaconId;
}