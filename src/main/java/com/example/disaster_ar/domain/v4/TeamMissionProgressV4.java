package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.ProgressStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "team_mission_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_team_progress_one", columnNames = {"scenario_id", "assignment_id", "team_id"})
        },
        indexes = {
                @Index(name = "idx_team_progress_team", columnList = "team_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TeamMissionProgressV4 {

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

    @Column(name = "required_count", nullable = false)
    private Integer requiredCount;

    @Column(name = "progress_count", nullable = false)
    private Integer progressCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProgressStatus status = ProgressStatus.IN_PROGRESS;

    @Column(name = "updated_at")
    private LocalDateTime updateAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}