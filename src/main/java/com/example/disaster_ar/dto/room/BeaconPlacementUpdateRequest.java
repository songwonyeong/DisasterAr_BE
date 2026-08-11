package com.example.disaster_ar.dto.room;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BeaconPlacementUpdateRequest {

    private Integer floorIndex;

    // 구조도 위 비콘 마커 element id
    // 예: beacon-1785307291071-rq8k
    private String beaconElementId;

    // 이 비콘이 대표하는 구역 element id
    // 예: auto-room-8
    private String zoneElementId;

    // 구조도 위 비콘 좌표
    private Double x;
    private Double y;

    // 선택값
    private Double realXM;
    private Double realYM;
    private Double realZM;

    private String placementName;
    private String zoneType;
    private Integer thresholdRssi;

    @JsonProperty("isActive")
    private Boolean isActive;
}