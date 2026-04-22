package com.example.disaster_ar.dto.beacon;

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}