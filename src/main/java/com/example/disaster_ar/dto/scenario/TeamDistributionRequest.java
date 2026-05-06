package com.example.disaster_ar.dto.scenario;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamDistributionRequest {

    // AUTO / MANUAL
    private String mode;

    private List<ManualTeamCount> manualTeamCounts;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ManualTeamCount {
        private String teamCode;
        private String teamName;
        private Integer maxMembers;
    }
}