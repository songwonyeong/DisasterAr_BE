package com.example.disaster_ar.dto.scenario;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TeamDistributionResponse {

    private String scenarioId;
    private String classroomId;
    private String scenarioType;
    private Integer totalStudents;
    private List<TeamResult> teams;

    @Getter
    @Builder
    public static class TeamResult {
        private String teamId;
        private String teamCode;
        private String teamName;
        private Integer minMembers;
        private Integer maxMembers;
    }
}