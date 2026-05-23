package com.example.disaster_ar.dto.beacon;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BeaconElementMapCreateRequest {
    private String schoolId;
    private Integer floorIndex;
    private String beaconId;

    // 기존 호환용
    private String elementId;

    // 신규
    private String beaconElementId;
    private String zoneElementId;
    private String placementName;
    private String zoneType;
    private Integer thresholdRssi;

    @JsonProperty("isActive")
    private Boolean isActive;
}