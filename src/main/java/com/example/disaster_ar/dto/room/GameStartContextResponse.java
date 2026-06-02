package com.example.disaster_ar.dto.room;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStartContextResponse {

    private String classroomId;

    private String scenarioId;
    private String scenarioType;

    private String studentId;
    private String teamId;
    private String teamCode;
    private String teamName;

    private String trainingState;
    private LocalDateTime trainingStartedAt;

    private String activeMapVersionId;
    private String floorsJson;

    private String npcPositionsJson;
    private String teamAssignmentJson;

    private Integer disasterOriginFloorIndex;
    private String disasterOriginElementId;
    private String disasterOriginName;
    private String disasterMessage;

    private String scenarioEventContentId;
    private String scenarioEventPlace;
    private String scenarioEventReason;

    private String warningMessage;

    private Integer trainingTimeSeconds;

}