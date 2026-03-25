package com.example.disaster_ar.domain.v4;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "channel_maps",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_school_floor", columnNames = {"school_id", "floor_index"})
        },
        indexes = {
                @Index(name = "idx_channel_maps_school", columnList = "school_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChannelMapV4 {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private SchoolV4 school;

    @Column(name = "floor_index", nullable = false)
    private Integer floorIndex;

    @Column(name = "floor_label", length = 50)
    private String floorLabel;

    @Column(name = "uploaded_image", length = 500)
    private String uploadedImage;

    @Column(name = "outline_json", columnDefinition = "json")
    private String outlineJson;

    @Column(name = "scale_m_per_px")
    private Double scaleMPerPx;

    @Column(name = "origin_x")
    private Double originX;

    @Column(name = "origin_y")
    private Double originY;

    @Column(name = "elements_json", nullable = false, columnDefinition = "json")
    private String elementsJson;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
}