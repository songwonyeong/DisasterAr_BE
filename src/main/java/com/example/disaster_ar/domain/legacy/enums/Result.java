package com.example.disaster_ar.domain.legacy.enums;

import com.example.disaster_ar.domain.legacy.Scenario;
import com.example.disaster_ar.domain.legacy.Student;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Result {

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

    @Column(name = "evacuation_time")
    private Integer evacuationTime;

    @Column(name = "missions_completed")
    private Integer missionsCompleted;

    @Column(name = "penalty_reason", length = 255)
    private String penaltyReason;

    @Column(name = "total_score")
    private Integer totalScore;
}
