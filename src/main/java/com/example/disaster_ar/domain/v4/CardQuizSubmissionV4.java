package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.CardQuizKind;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "card_quiz_submissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_card_quiz_one_submit",
                        columnNames = {"scenario_id", "assignment_id", "student_id", "quiz_kind"}
                )
        },
        indexes = {
                @Index(name = "idx_card_quiz_student", columnList = "student_id"),
                @Index(name = "idx_card_quiz_assignment", columnList = "assignment_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CardQuizSubmissionV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private ScenarioV4 scenario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private ScenarioAssignmentV4 assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentV4 student;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_kind", nullable = false, length = 20)
    private CardQuizKind quizKind;

    @Column(name = "selected_order_json", nullable = false, columnDefinition = "json")
    private String selectedOrderJson;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
}