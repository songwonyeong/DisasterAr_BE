package com.example.disaster_ar.domain.v4;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "school_map_templates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_school_template_name", columnNames = {"school_id", "template_name"})
        },
        indexes = {
                @Index(name = "idx_templates_school", columnList = "school_id"),
                @Index(name = "idx_templates_parent", columnList = "parent_template_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SchoolMapTemplateV4 extends BaseTimeEntity {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private SchoolV4 school;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "description", length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_template_id")
    private SchoolMapTemplateV4 parentTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserV4 createdBy;

    @Column(name = "floors_json", nullable = false, columnDefinition = "json")
    private String floorsJson;
}