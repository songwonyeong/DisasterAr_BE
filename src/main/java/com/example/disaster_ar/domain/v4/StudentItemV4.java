package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.AcquiredSource;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_student_one_item_in_scenario",
                        columnNames = {"scenario_id", "student_id", "item_id"}
                )
        },
        indexes = {
                @Index(name = "idx_student_items_student", columnList = "student_id"),
                @Index(name = "idx_student_items_item", columnList = "item_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentItemV4 {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private ScenarioV4 scenario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentV4 student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemV4 item;

    @Builder.Default
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "acquired_at")
    private LocalDateTime acquiredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "acquired_source", length = 20)
    private AcquiredSource acquiredSource;

    @Builder.Default
    @Column(name = "is_consumed", nullable = false)
    private Boolean isConsumed = false;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;
}