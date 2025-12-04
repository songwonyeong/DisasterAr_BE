package com.example.disaster_ar.domain;

import com.example.disaster_ar.domain.enums.StudentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 64)
    private String id;

    // classroom_id FK NOT NULL
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(name = "student_name", nullable = false, length = 255)
    private String studentName;

    // ENUM('EVACUATING','EVACUATED','RESTRICTED','UNKNOWN')
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StudentStatus status;

    @Column(name = "position_x")
    private Double positionX;

    @Column(name = "position_y")
    private Double positionY;

    @Column(name = "is_kicked")
    private Boolean isKicked;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = StudentStatus.UNKNOWN;
        }
        if (isKicked == null) {
            isKicked = false;
        }
        if (lastUpdate == null) {
            lastUpdate = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        lastUpdate = LocalDateTime.now();
    }
}
