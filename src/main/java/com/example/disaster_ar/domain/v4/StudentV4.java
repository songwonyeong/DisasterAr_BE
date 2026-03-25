package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.BeaconState;
import com.example.disaster_ar.domain.v4.enums.StudentStatus;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StudentV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false)
    private ClassroomV4 classroom;

    @Column(name = "student_name", length = 100)
    private String studentName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StudentStatus status = StudentStatus.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(name = "beacon_state", length = 20)
    private BeaconState beaconState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_beacon_id")
    private BeaconV4 lastBeacon;

    @Column(name = "last_beacon_rssi")
    private Integer lastBeaconRssi;

    @Column(name = "last_beacon_seen_at")
    private LocalDateTime lastBeaconSeenAt;

    @Builder.Default
    @Column(name = "is_kicked", nullable = false)
    private Boolean isKicked = false;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;
}