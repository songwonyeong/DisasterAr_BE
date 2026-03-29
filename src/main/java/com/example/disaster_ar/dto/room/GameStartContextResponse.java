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

    private String trainingState;
    private LocalDateTime trainingStartedAt;

    private String activeMapVersionId;
    private String floorsJson;

    private String npcPositionsJson;
    private String teamAssignmentJson;
}