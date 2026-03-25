package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.TriggerReason;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "scenario_triggers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_once_per_student_assignment",
                        columnNames = {"scenario_id", "student_id", "assignment_id"}
                )
        },
        indexes = {
                @Index(name = "idx_triggers_assignment", columnList = "assignment_id"),
                @Index(name = "idx_triggers_student", columnList = "student_id"),
                @Index(name = "idx_triggers_scenario", columnList = "scenario_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ScenarioTriggerV4 {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_reason", nullable = false, length = 30)
    private TriggerReason triggerReason;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "status", length = 20)
    private String status; // TRIGGERED|COMPLETED|EXPIRED|FAILED

    @Column(name = "payload_json", columnDefinition = "json")
    private String payloadJson;
}