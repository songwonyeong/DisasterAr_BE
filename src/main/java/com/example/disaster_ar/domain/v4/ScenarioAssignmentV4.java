package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.AssignmentType;
import com.example.disaster_ar.domain.v4.enums.TargetType;
import com.example.disaster_ar.domain.v4.enums.ActorType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "scenario_assignments",
        indexes = {
                @Index(name = "idx_assignments_scenario", columnList = "scenario_id"),
                @Index(name = "idx_assignments_classroom_floor", columnList = "classroom_id, floor_index"),
                @Index(name = "idx_assignments_content", columnList = "content_id"),
                @Index(name = "idx_assignments_target_team", columnList = "target_team_id"),
                @Index(name = "idx_assignments_beacon", columnList = "beacon_id"),
                @Index(name = "idx_assignments_created_by_user", columnList = "created_by_user_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ScenarioAssignmentV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private ScenarioV4 scenario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false)
    private ClassroomV4 classroom;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 20)
    private AssignmentType assignmentType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private ContentV4 content;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20)
    private TargetType targetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_team_id")
    private ScenarioTeamV4 targetTeam;

    @Column(name = "floor_index", nullable = false)
    private Integer floorIndex;

    @Column(name = "element_id", length = 64)
    private String elementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beacon_id")
    private BeaconV4 beacon;

    @Column(name = "params_json", columnDefinition = "json")
    private String paramsJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_by_type", length = 20)
    private ActorType createdByType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private UserV4 createdByUser;
}