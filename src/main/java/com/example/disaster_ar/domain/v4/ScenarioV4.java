package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.NpcMode;
import com.example.disaster_ar.domain.v4.enums.ScenarioType;
import com.example.disaster_ar.domain.v4.enums.TeamMode;
import com.example.disaster_ar.domain.v4.enums.TriggerMode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scenarios", indexes = {
        @Index(name = "idx_scenarios_classroom", columnList = "classroom_id"),
        @Index(name = "idx_scenarios_created_time", columnList = "created_time"),
        @Index(name = "idx_scenarios_map_version", columnList = "map_version_id"),
        @Index(name = "idx_scenarios_selected_event", columnList = "selected_scenario_event_content_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ScenarioV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false)
    private ClassroomV4 classroom;

    @Column(name = "scenario_name", length = 100)
    private String scenarioName;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario_type", nullable = false, length = 20)
    private ScenarioType scenarioType;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_mode", length = 20)
    private TriggerMode triggerMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_mode", length = 20)
    private TeamMode teamMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "npc_mode", length = 20)
    private NpcMode npcMode;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "intensity")
    private Integer intensity;

    @Column(name = "train_time")
    private Integer trainTime;

    @Column(name = "participant_count")
    private Integer participantCount;

    @Column(name = "created_time")
    private java.time.LocalDateTime createdTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_version_id")
    private RoomMapVersionV4 mapVersion;

    @Column(name = "disaster_origin_floor_index")
    private Integer disasterOriginFloorIndex;

    @Column(name = "disaster_origin_element_id", length = 64)
    private String disasterOriginElementId;

    @Column(name = "disaster_origin_name", length = 255)
    private String disasterOriginName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_scenario_event_content_id")
    private ContentV4 selectedScenarioEventContent;

    @Column(name = "ai_decision_json", columnDefinition = "json")
    private String aiDecisionJson;

    @Column(name = "team_assignment", columnDefinition = "json")
    private String teamAssignmentJson;

    @Column(name = "npc_positions", columnDefinition = "json")
    private String npcPositionsJson;
}