package com.example.disaster_ar.dto.scenario;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TeamDistributionRequest {

    private String distributionMode; // AUTO / MANUAL

    private List<ManualTeamCount> manualTeamCounts;

    @Getter
    @Setter
    public static class ManualTeamCount {
        private String teamCode;
        private String teamName;
        private Integer maxMembers;
    }
}