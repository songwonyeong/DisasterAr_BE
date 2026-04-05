package com.example.disaster_ar.dto.room;

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
public class StudentRoomResponse {
    private String studentId;
    private String studentName;
    private LocalDateTime joinedAt;
    private String status;
    private Boolean isKicked;

    private Integer floorIndex;
    private Double x;
    private Double y;
    private String beaconId;
    private Integer lastRssi;
    private String lastSeenAt;
}