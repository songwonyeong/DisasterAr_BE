package com.example.disaster_ar.domain.v4;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "schools",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_schools_name", columnNames = "school_name"),
                @UniqueConstraint(name = "uq_schools_access_code", columnNames = "access_code")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SchoolV4 extends BaseTimeEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "school_name", nullable = false, length = 255)
    private String schoolName;

    @Column(name = "access_code", nullable = false, length = 50)
    private String accessCode;

    @Column(name = "thumbnail_image", length = 500)
    private String thumbnailImage;
}