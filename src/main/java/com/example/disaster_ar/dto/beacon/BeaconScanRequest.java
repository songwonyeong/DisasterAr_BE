package com.example.disaster_ar.dto.beacon;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BeaconScanRequest {
    private String studentId;
    private String classroomId;
    private List<BeaconSignal> scans;
    private String scannedAt;
}