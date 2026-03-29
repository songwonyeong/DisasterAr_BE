package com.example.disaster_ar.dto.beacon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeaconResponse {
    private String beaconId;
    private String schoolId;
    private Integer floorIndex;
    private String uuid;
    private Integer major;
    private Integer minor;
    private Integer beaconNo;
    private String mac;
    private Double x;
    private Double y;
    private Double realXM;
    private Double realYM;
    private Double realZM;
    private String name;
    private Integer txPower;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}