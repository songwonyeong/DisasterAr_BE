package com.example.disaster_ar.dto.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    /**
     * 기존 호환용.
     * 1차에서는 zoneElementId와 같은 값으로 내려준다.
     */
    private String elementId;

    /**
     * 구조도 JSON 안의 비콘 마커 element ID.
     * 좌표 표시용.
     */
    private String beaconElementId;

    /**
     * SAFE_ZONE / DISASTER_ZONE / RESTRICTED_ZONE 같은 구역 element ID.
     * 미션/트리거 기준.
     */
    private String zoneElementId;

    private String placementName;
    private String zoneType;
    private Integer thresholdRssi;

    @JsonProperty("isActive")
    private Boolean isActive;

    private Double x;
    private Double y;
    private Double width;
    private Double height;

    private Integer studentCount;
    private List<MonitoringStudentResponse> students;
}