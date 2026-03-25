package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.TeamStepStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "team_mission_step_progress",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_team_step_one",
                        columnNames = {"scenario_id", "assignment_id", "team_id", "step_order"}
                )
        },
        indexes = {
                @Index(name = "idx_tmsp_scenario", columnList = "scenario_id"),
                @Index(name = "idx_tmsp_assignment", columnList = "assignment_id"),
                @Index(name = "idx_tmsp_team", columnList = "team_id"),
                @Index(name = "idx_tmsp_type", columnList = "step_type")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TeamMissionStepProgressV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private ScenarioV4 scenario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private ScenarioAssignmentV4 assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private ScenarioTeamV4 team;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "step_type", nullable = false, length = 50)
    private String stepType;

    @Column(name = "step_name", length = 100)
    private String stepName;

    @Column(name = "required_count", nullable = false)
    private Integer requiredCount = 1;

    @Column(name = "progress_count", nullable = false)
    private Integer progressCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TeamStepStatus status = TeamStepStatus.LOCKED;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "meta_json", columnDefinition = "json")
    private String metaJson;
}