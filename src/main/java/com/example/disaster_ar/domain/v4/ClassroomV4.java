package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.TrainingState;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "classrooms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_classroom_join_code", columnNames = "join_code")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ClassroomV4 extends BaseTimeEntity {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private SchoolV4 school;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private UserV4 owner;

    @Column(name = "class_name", length = 100)
    private String className;

    @Column(name = "student_count")
    private Integer studentCount;

    @Column(name = "join_code", nullable = false, length = 50)
    private String joinCode;

    // V4: classrooms.active_map_version_id -> room_map_versions.id (SET NULL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_map_version_id")
    private RoomMapVersionV4 activeMapVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "training_state", nullable = false, length = 20)
    private TrainingState trainingState = TrainingState.WAITING;

    @Column(name = "training_started_at")
    private LocalDateTime trainingStartedAt;

    @Column(name = "training_ended_at")
    private LocalDateTime trainingEndedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_scenario_id")
    private ScenarioV4 activeScenario;
}