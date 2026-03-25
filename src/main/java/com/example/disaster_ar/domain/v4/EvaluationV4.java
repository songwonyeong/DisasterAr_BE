package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.EvalLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "evaluations",
        indexes = {
                @Index(name = "idx_eval_scenario_level", columnList = "scenario_id, level"),
                @Index(name = "idx_eval_student", columnList = "student_id"),
                @Index(name = "idx_eval_team", columnList = "team_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EvaluationV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private ScenarioV4 scenario;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    private EvalLevel level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private StudentV4 student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private ScenarioTeamV4 team;

    @Column(name = "score_total")
    private Double scoreTotal;

    @Column(name = "score_json", columnDefinition = "json")
    private String scoreJson;

    @Column(name = "feedback_text", columnDefinition = "text")
    private String feedbackText;

    @Column(name = "details_json", columnDefinition = "json")
    private String detailsJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}