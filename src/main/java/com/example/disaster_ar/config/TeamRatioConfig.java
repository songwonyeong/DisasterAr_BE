package com.example.disaster_ar.config;

import com.example.disaster_ar.domain.v4.enums.ScenarioType;

import java.util.List;
import java.util.Map;

public class TeamRatioConfig {

    public record TeamRatio(
            String teamCode,
            String teamName,
            int ratio
    ) {}

    public static final Map<ScenarioType, List<TeamRatio>> TEAM_RATIO_BY_SCENARIO = Map.of(
            ScenarioType.FIRE, List.of(
                    new TeamRatio("CIVILIAN", "시민팀", 5),
                    new TeamRatio("FIRE_FIGHT", "소화팀", 3),
                    new TeamRatio("FIRST_AID", "응급처치팀", 2)
            ),
            ScenarioType.EARTHQUAKE, List.of(
                    new TeamRatio("EVAC", "대피팀", 6),
                    new TeamRatio("RESCUE", "구조팀", 2),
                    new TeamRatio("MEDICAL", "응급처치팀", 2)
            ),
            ScenarioType.CHEMICAL, List.of(
                    new TeamRatio("EVAC", "대피팀", 4),
                    new TeamRatio("CONTROL", "확산차단팀", 4),
                    new TeamRatio("MEDICAL", "응급처치팀", 2)
            )
    );

    private TeamRatioConfig() {
    }
}