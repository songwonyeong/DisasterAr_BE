package com.example.disaster_ar.dto.scenario;

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
public class TriggeredAssignmentResponse {
    private String triggerId;
    private String assignmentId;
    private String assignmentType;
    private String status;
    private String triggerReason;
    private LocalDateTime triggeredAt;

    private String contentId;
    private String title;
    private String description;

    private Integer floorIndex;
    private String elementId;
    private String beaconId;
}