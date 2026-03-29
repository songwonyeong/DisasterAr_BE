package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.ProgressStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_mission_progress",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_student_progress_one",
                        columnNames = {"scenario_id", "assignment_id", "student_id"}
                )
        },
        indexes = {
                @Index(name = "idx_smp_student", columnList = "student_id"),
                @Index(name = "idx_smp_assignment", columnList = "assignment_id"),
                @Index(name = "idx_smp_scenario", columnList = "scenario_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StudentMissionProgressV4 {

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
    @JoinColumn(name = "student_id", nullable = false)
    private StudentV4 student;

    @Column(name = "required_count", nullable = false)
    private Integer requiredCount;

    @Builder.Default
    @Column(name = "progress_count", nullable = false)
    private Integer progressCount = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProgressStatus status = ProgressStatus.IN_PROGRESS;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "progress_json", columnDefinition = "json")
    private String progressJson;
}