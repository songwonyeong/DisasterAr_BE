package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.QuizResultStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "quiz_submissions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_quiz_one_submit", columnNames = {"scenario_id", "assignment_id", "student_id"})
        },
        indexes = {
                @Index(name = "idx_quiz_by_student", columnList = "scenario_id, student_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class QuizSubmissionV4 {

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

    @Column(name = "selected_answer", nullable = false)
    private Integer selectedAnswer;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QuizResultStatus status;
}