package com.example.disaster_ar.dto.beacon;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class BeaconElementMapResponse {

    private String id;
    private String schoolId;
    private Integer floorIndex;
    private String beaconId;
    private String beaconName;

    private String elementId;

    private String beaconElementId;
    private String zoneElementId;
    private String placementName;
    private String zoneType;
    private Integer thresholdRssi;

    @JsonProperty("isActive")
    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}