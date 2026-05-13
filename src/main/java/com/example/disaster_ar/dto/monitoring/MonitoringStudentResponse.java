package com.example.disaster_ar.dto.monitoring;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringStudentResponse {

    private String studentId;
    private String studentName;

    private String beaconState;
    private Integer lastRssi;
    private String lastSeenAt;
}