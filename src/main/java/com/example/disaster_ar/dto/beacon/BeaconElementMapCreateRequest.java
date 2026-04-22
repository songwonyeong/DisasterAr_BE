package com.example.disaster_ar.dto.beacon;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BeaconElementMapCreateRequest {
    private String schoolId;
    private Integer floorIndex;
    private String beaconId;
    private String elementId;
}