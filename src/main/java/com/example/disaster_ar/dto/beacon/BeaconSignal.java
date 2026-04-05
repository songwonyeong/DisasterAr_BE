package com.example.disaster_ar.dto.beacon;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BeaconSignal {
    private String uuid;
    private int major;
    private int minor;
    private int rssi;
}