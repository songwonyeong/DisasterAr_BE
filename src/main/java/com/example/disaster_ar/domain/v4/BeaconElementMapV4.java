package com.example.disaster_ar.domain.v4;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "beacon_element_maps",
        indexes = {
                @Index(name = "idx_beacon_element_maps_school_floor", columnList = "school_id, floor_index"),
                @Index(name = "idx_beacon_element_maps_element", columnList = "school_id, floor_index, element_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_beacon_element_maps_beacon", columnNames = {"beacon_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeaconElementMapV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private SchoolV4 school;

    @Column(name = "floor_index", nullable = false)
    private Integer floorIndex;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beacon_id", nullable = false)
    private BeaconV4 beacon;

    @Column(name = "element_id", nullable = false, length = 64)
    private String elementId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}