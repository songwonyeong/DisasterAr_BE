package com.example.disaster_ar.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

import com.example.disaster_ar.domain.enums.ScenarioType;
import com.example.disaster_ar.domain.enums.TriggerMode;
import com.example.disaster_ar.domain.enums.TeamMode;
import com.example.disaster_ar.domain.enums.NpcMode;

@Entity
@Table(name = "scenarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 64)
    private String id;

    // classroom_id FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    // ★ 시나리오 이름 (신규)
    @Column(name = "scenario_name", nullable = false, length = 255)
    private String scenarioName;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario_type", nullable = false, length = 20)
    private ScenarioType scenarioType;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_mode", nullable = false, length = 20)
    private TriggerMode triggerMode;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "intensity")
    private Integer intensity;

    @Column(name = "train_time")
    private Integer trainTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_mode", nullable = false, length = 20)
    private TeamMode teamMode;

    // JSON 컬럼 (MySQL 8.x)
    @Column(name = "team_assignment", columnDefinition = "json")
    private String teamAssignment;

    @Enumerated(EnumType.STRING)
    @Column(name = "npc_mode", nullable = false, length = 20)
    private NpcMode npcMode;

    @Column(name = "npc_positions", columnDefinition = "json")
    private String npcPositions;

    @Column(name = "participant_count")
    private Integer participantCount;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    // ★ 시나리오-아이콘 역관계
    @Builder.Default
    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MapIcon> mapIcons = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.triggerMode == null) {
            this.triggerMode = TriggerMode.AUTO;
        }
        if (this.teamMode == null) {
            this.teamMode = TeamMode.AUTO;
        }
        if (this.npcMode == null) {
            this.npcMode = NpcMode.AUTO;
        }
        if (this.createdTime == null) {
            this.createdTime = LocalDateTime.now();
        }
    }
}
