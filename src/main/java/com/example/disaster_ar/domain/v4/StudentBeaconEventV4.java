package com.example.disaster_ar.domain.v4;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_beacon_events",
        indexes = {
                @Index(name = "idx_beacon_events_student_time", columnList = "scenario_id, student_id, event_at"),
                @Index(name = "idx_beacon_events_to_beacon_time", columnList = "to_beacon_id, event_at")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StudentBeaconEventV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private ScenarioV4 scenario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentV4 student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_beacon_id")
    private BeaconV4 fromBeacon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_beacon_id")
    private BeaconV4 toBeacon;

    @Column(name = "rssi")
    private Integer rssi;

    @Column(name = "event_at", nullable = false)
    private LocalDateTime eventAt;
}