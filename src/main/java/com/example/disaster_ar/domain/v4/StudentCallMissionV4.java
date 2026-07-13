package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.CallMissionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_call_missions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_student_call_mission_session",
                        columnNames = {"scenario_id", "training_session_id", "student_id"}
                )
        },
        indexes = {
                @Index(name = "idx_call_mission_scenario_session", columnList = "scenario_id, training_session_id"),
                @Index(name = "idx_call_mission_student", columnList = "student_id"),
                @Index(name = "idx_call_mission_classroom", columnList = "classroom_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentCallMissionV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private ScenarioV4 scenario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false)
    private ClassroomV4 classroom;

    @Column(name = "training_session_id", nullable = false, length = 36)
    private String trainingSessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentV4 student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private ScenarioAssignmentV4 assignment;

    @Column(name = "trigger_offset_seconds", nullable = false)
    private Integer triggerOffsetSeconds;

    @Column(name = "available_from")
    private LocalDateTime availableFrom;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private CallMissionStatus status = CallMissionStatus.SCHEDULED;

    @Column(name = "call_started_at")
    private LocalDateTime callStartedAt;

    @Column(name = "call_ended_at")
    private LocalDateTime callEndedAt;

    @Column(name = "teacher_judged_at")
    private LocalDateTime teacherJudgedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_judged_by")
    private UserV4 teacherJudgedBy;

    @Column(name = "teacher_judgement_memo", length = 500)
    private String teacherJudgementMemo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
