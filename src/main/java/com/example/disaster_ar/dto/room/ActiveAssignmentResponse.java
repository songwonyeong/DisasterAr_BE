package com.example.disaster_ar.dto.room;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ActiveAssignmentResponse {
    private String triggerId;
    private String assignmentId;
    private String assignmentType;

    private String contentId;
    private String title;
    private String description;

    private String targetType;
    private String targetTeamId;

    private Integer floorIndex;
    private String beaconId;

    private String paramsJson;
    private String missionCode;

    private LocalDateTime triggeredAt;
}