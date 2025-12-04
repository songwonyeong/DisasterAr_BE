package com.example.disaster_ar.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "map_icons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MapIcon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 64)
    private String id;

    // map_id FK → school_maps.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_id")
    private SchoolMap map;

    // ★ 새로 추가: scenario_id FK → scenarios.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id")
    private Scenario scenario;

    @Column(name = "icon_type", length = 255)
    private String iconType;

    @Column(name = "x_cord")
    private Double xCoord;

    @Column(name = "y_cord")
    private Double yCoord;

    @Column(name = "floor", length = 50)
    private String floor;
}
