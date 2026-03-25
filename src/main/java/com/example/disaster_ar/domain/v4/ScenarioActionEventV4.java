package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.ScenarioActionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "scenario_action_events",
        indexes = {
                @Index(name = "idx_action_scenario_time", columnList = "scenario_id, created_at"),
                @Index(name = "idx_action_student_time", columnList = "student_id, created_at"),
                @Index(name = "idx_action_classroom_time", columnList = "classroom_id, created_at"),
                @Index(name = "idx_action_type_time", columnList = "action_type, created_at")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ScenarioActionEventV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private ScenarioV4 scenario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id")
    private ClassroomV4 classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private StudentV4 student;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private ScenarioActionType actionType;

    @Column(name = "floor_index")
    private Integer floorIndex;

    @Column(name = "element_id", length = 64)
    private String elementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beacon_id")
    private BeaconV4 beacon;

    @Column(name = "value_int")
    private Integer valueInt;

    @Column(name = "value_text", length = 255)
    private String valueText;

    @Column(name = "meta_json", columnDefinition = "json")
    private String metaJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}