package com.example.disaster_ar.domain;

import com.example.disaster_ar.domain.enums.MonitoringStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "monitoring")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Monitoring {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 64)
    private String id;

    // scenario_id FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id")
    private Scenario scenario;

    // student_id FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "x")
    private Double x;

    @Column(name = "y")
    private Double y;

    // ENUM 기반 상태값
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private MonitoringStatus status;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (status == null) {
            status = MonitoringStatus.ACTIVE;
        }
    }
}
