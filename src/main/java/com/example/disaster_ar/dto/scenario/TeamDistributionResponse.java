package com.example.disaster_ar.dto.scenario;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamDistributionResponse {

    private String scenarioId;
    private String classroomId;
    private String scenarioType;
    private Integer totalStudents;

    private List<TeamResult> teams;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamResult {
        private String teamId;
        private String teamCode;
        private String teamName;
        private Integer minMembers;
        private Integer maxMembers;
    }
}