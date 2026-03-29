package com.example.disaster_ar.dto.beacon;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BeaconRequest {
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
}