package com.example.disaster_ar.dto.monitoring;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringFloorResponse {

    private Integer floorIndex;
    private String floorLabel;

    private MonitoringImageResponse image;

    // 구조도 JSON 원본 elements
    private List<Map<String, Object>> elements;

    // 프론트 마커 표시용
    private List<BeaconMarkerResponse> beaconMarkers;
}