package com.example.disaster_ar.domain.v4;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "channel_element_tags",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_school_floor_element",
                        columnNames = {"school_id", "floor_index", "element_id"}
                )
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChannelElementTagV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private SchoolV4 school;

    @Column(name = "floor_index", nullable = false)
    private Integer floorIndex;

    @Column(name = "element_id", nullable = false, length = 64)
    private String elementId;

    @Column(name = "element_type", length = 50)
    private String elementType;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "tags_json", columnDefinition = "json")
    private String tagsJson;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}