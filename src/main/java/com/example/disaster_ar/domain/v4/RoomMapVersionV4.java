package com.example.disaster_ar.domain.v4;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "room_map_versions",
        indexes = {
                @Index(name = "idx_room_versions_classroom", columnList = "classroom_id"),
                @Index(name = "idx_room_versions_school", columnList = "school_id"),
                @Index(name = "idx_room_versions_source_template", columnList = "source_template_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RoomMapVersionV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false)
    private ClassroomV4 classroom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private SchoolV4 school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_template_id")
    private SchoolMapTemplateV4 sourceTemplate;

    @Column(name = "label", length = 100)
    private String label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserV4 createdBy;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "floors_json", nullable = false, columnDefinition = "json")
    private String floorsJson;
}