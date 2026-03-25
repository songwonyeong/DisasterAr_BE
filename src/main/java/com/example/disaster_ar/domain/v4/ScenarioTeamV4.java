package com.example.disaster_ar.domain.v4;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "scenario_teams",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_scenario_team_code", columnNames = {"scenario_id", "team_code"})
        },
        indexes = {
                @Index(name = "idx_scenario_teams_scenario", columnList = "scenario_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ScenarioTeamV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private ScenarioV4 scenario;

    @Column(name = "team_code", nullable = false, length = 20)
    private String teamCode;

    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "min_members")
    private Integer minMembers;

    @Column(name = "max_members")
    private Integer maxMembers;
}