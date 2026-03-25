package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.ActorType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "scenario_team_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_scenario_student_one_team", columnNames = {"scenario_id", "student_id"})
        },
        indexes = {
                @Index(name = "idx_team_members_team", columnList = "team_id"),
                @Index(name = "idx_team_members_scenario", columnList = "scenario_id"),
                @Index(name = "idx_team_members_assigned_by_user", columnList = "assigned_by_user_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ScenarioTeamMemberV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private ScenarioV4 scenario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentV4 student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private ScenarioTeamV4 team;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_by_type", length = 20)
    private ActorType assignedByType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id")
    private UserV4 assignedByUser;
}