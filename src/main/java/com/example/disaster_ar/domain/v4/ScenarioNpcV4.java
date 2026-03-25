package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.NpcStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "scenario_npcs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_npc_code_in_scenario", columnNames = {"scenario_id", "npc_code"})
        },
        indexes = {
                @Index(name = "idx_npcs_scenario", columnList = "scenario_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ScenarioNpcV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private ScenarioV4 scenario;

    @Column(name = "npc_code", length = 64)
    private String npcCode;

    @Column(name = "floor_index", nullable = false)
    private Integer floorIndex;

    @Column(name = "element_id", length = 64)
    private String elementId;

    @Column(name = "x")
    private Double x;

    @Column(name = "y")
    private Double y;

    @Column(name = "role", length = 50)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private NpcStatus status;

    @Column(name = "meta_json", columnDefinition = "json")
    private String metaJson;
}